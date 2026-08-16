import type {
  NextLineResponse,
  RequestNextLineResponse,
  SimulationFeedback,
  SimulationFlowFailReason,
  SimulationFlowSnapshot,
  SimulationStartResponse,
  SubmitEvaluationResponse,
  TurnEvaluationResponse,
} from "../../types";
import { startPolling } from "../polling/poller";
import type { PollingHandle } from "../polling/poller";
import {
  EVALUATION_FALLBACK_FEEDBACK,
  SIMULATION_POLL_INTERVAL_MS,
  SIMULATION_POLL_MAX_CONSECUTIVE_ERRORS,
  SIMULATION_POLL_TIMEOUT_MS,
  SIMULATION_TURN_FEEDBACK_LINGER_MS,
} from "./constants";

export interface SimulationFlowApi {
  start: () => Promise<SimulationStartResponse>;
  submitEvaluation: (
    turnNo: number,
    transcript: string,
  ) => Promise<SubmitEvaluationResponse>;
  getEvaluation: (turnNo: number) => Promise<TurnEvaluationResponse>;
  requestNextLine: (turnNo: number) => Promise<RequestNextLineResponse>;
  getNextLine: (turnNo: number) => Promise<NextLineResponse>;
}

export interface SimulationFlowControllerOptions {
  api: SimulationFlowApi;
  onChange: (snapshot: SimulationFlowSnapshot) => void;
  pollIntervalMs?: number;
  pollTimeoutMs?: number;
  maxConsecutivePollErrors?: number;
  turnFeedbackLingerMs?: number;
}

type RetryStep =
  | { kind: "START" }
  | { kind: "EVALUATION"; transcript: string }
  | { kind: "NEXT_LINE"; turnNo: number };

/**
 * 시뮬레이션 스테이지의 턴 진행 상태머신 (React 무관 순수 로직 —
 * BriefingFlowController와 같은 구조: 컨트롤러가 상태를 소유하고 매 변화를
 * 불변 스냅샷으로 통지한다. 훅은 얇은 바인딩만 한다).
 *
 * 상태 전이는 SimulationFlowStatus 타입 주석 참고. 핵심 규칙:
 * - 턴은 스테이지 내부 상태다: 첫 실패는 같은 턴을 한 번 재시도하고,
 *   두 번째 실패나 AI fallback은 실패 결과를 유지한 채 다음 턴으로 전진한다.
 * - 종료 판정은 프론트 책임: currentTurn == maxTurn에서 턴이 완료되면 next-line을
 *   요청하지 않고 COMPLETED로 끝낸다 (초과 요청은 서버가 409로 거부).
 * - 판정 워커 장애(status=FAILED)는 flow 실패가 아니라 "실패 판정 + 고정
 *   피드백"으로 흡수한다 — 전시 안 멈춤. 서버는 FAILED attempt 뒤 재제출을
 *   새 시도로 받아준다.
 * - FAILED에서 retry()는 마지막 단계(start/판정 제출/다음 발화 요청)를
 *   그대로 다시 밟는다. 202 이후 폴링만 실패한 경우에도 POST부터 다시 하지만,
 *   서버가 진행 중·완료된 작업을 그대로 돌려주므로(idempotent) 안전하다.
 */
export class SimulationFlowController {
  private readonly api: SimulationFlowApi;
  private readonly onChange: (snapshot: SimulationFlowSnapshot) => void;
  private readonly pollIntervalMs: number;
  private readonly pollTimeoutMs: number;
  private readonly maxConsecutivePollErrors: number;
  private readonly turnFeedbackLingerMs: number;

  private status: SimulationFlowSnapshot["status"] = "STARTING";
  private currentTurn = 0;
  private maxTurn = 0;
  private sceneCue: string | null = null;
  private opponentLine: string | null = null;
  private actionPrompt: string | null = null;
  private generationMode: SimulationFlowSnapshot["generationMode"] = null;
  private transcript: string | null = null;
  private evaluation: SimulationFeedback | null = null;
  private failReason: SimulationFlowFailReason | null = null;

  private lastStep: RetryStep = { kind: "START" };
  private begun = false;
  private polling: PollingHandle<
    TurnEvaluationResponse | NextLineResponse
  > | null = null;
  private feedbackLingerTimer: ReturnType<typeof setTimeout> | null = null;
  private disposed = false;

  constructor(options: SimulationFlowControllerOptions) {
    this.api = options.api;
    this.onChange = options.onChange;
    this.pollIntervalMs = options.pollIntervalMs ?? SIMULATION_POLL_INTERVAL_MS;
    this.pollTimeoutMs = options.pollTimeoutMs ?? SIMULATION_POLL_TIMEOUT_MS;
    this.maxConsecutivePollErrors =
      options.maxConsecutivePollErrors ??
      SIMULATION_POLL_MAX_CONSECUTIVE_ERRORS;
    this.turnFeedbackLingerMs =
      options.turnFeedbackLingerMs ?? SIMULATION_TURN_FEEDBACK_LINGER_MS;
  }

  begin(): void {
    if (this.disposed || this.begun) return;
    this.begun = true;
    this.runStart();
  }

  submitAnswer(transcript: string): void {
    if (this.disposed || this.status !== "ANSWERING") return;
    this.runEvaluation(transcript);
  }

  retry(): void {
    if (this.disposed || this.status !== "FAILED") return;
    switch (this.lastStep.kind) {
      case "START":
        this.runStart();
        break;
      case "EVALUATION":
        this.runEvaluation(this.lastStep.transcript);
        break;
      case "NEXT_LINE":
        this.runNextLine(this.lastStep.turnNo);
        break;
    }
  }

  dispose(): void {
    this.disposed = true;
    this.polling?.cancel();
    this.polling = null;
    if (this.feedbackLingerTimer !== null) {
      clearTimeout(this.feedbackLingerTimer);
      this.feedbackLingerTimer = null;
    }
  }

  private runStart(): void {
    this.lastStep = { kind: "START" };
    this.update({ status: "STARTING", failReason: null });
    this.api.start().then(
      (response) => {
        if (this.disposed) return;
        this.maxTurn = response.maxTurn;
        this.enterTurn(
          response.currentTurn,
          response.sceneCue,
          response.opponentLine,
          response.actionPrompt,
          response.generationMode,
        );
      },
      () => {
        if (!this.disposed) this.fail("NETWORK");
      },
    );
  }

  private runEvaluation(transcript: string): void {
    const turnNo = this.currentTurn;
    this.lastStep = { kind: "EVALUATION", transcript };
    this.update({
      status: "EVALUATING",
      transcript,
      evaluation: null,
      failReason: null,
    });
    this.api.submitEvaluation(turnNo, transcript).then(
      () => {
        if (!this.disposed) this.pollEvaluation(turnNo);
      },
      () => {
        if (!this.disposed) this.fail("NETWORK");
      },
    );
  }

  private pollEvaluation(turnNo: number): void {
    this.startJobPolling(
      () => this.api.getEvaluation(turnNo),
      (response) => {
        if (response.status === "FAILED") {
          // 판정 워커 장애 — 실패 판정 + 고정 피드백으로 흡수한다(전시 안 멈춤).
          this.handleEvaluationOutcome({
            outcome: "RETRY_REQUIRED",
            feedback: EVALUATION_FALLBACK_FEEDBACK,
            fallback: true,
            turnCompleted: false,
          });
          return;
        }
        this.handleEvaluationOutcome({
          outcome: response.outcome ?? "RETRY_REQUIRED",
          feedback: response.feedback ?? EVALUATION_FALLBACK_FEEDBACK,
          fallback: response.fallback ?? false,
          turnCompleted: response.turnCompleted,
        });
      },
    );
  }

  private handleEvaluationOutcome(feedback: SimulationFeedback): void {
    if (!feedback.turnCompleted) {
      // 같은 턴 재시도 — 상대 발화는 그대로 두고 피드백만 얹는다.
      this.update({ status: "ANSWERING", evaluation: feedback });
      return;
    }
    if (this.currentTurn >= this.maxTurn) {
      this.update({ status: "COMPLETED", evaluation: feedback });
      return;
    }
    // 다음 발화를 요청하기 전, 방금 받은 피드백을 최소 시간 동안 화면에
    // 붙잡아 둔다 — 그대로 바로 요청하면 폴링이 빨리 끝날 때 피드백이
    // 뜨자마자 다음 턴 화면에 덮여 스치듯 사라진다.
    const nextTurnNo = this.currentTurn + 1;
    this.update({
      status: "NEXT_LINE",
      evaluation: feedback,
      failReason: null,
    });
    this.feedbackLingerTimer = setTimeout(() => {
      this.feedbackLingerTimer = null;
      if (this.disposed) return;
      this.runNextLine(nextTurnNo);
    }, this.turnFeedbackLingerMs);
  }

  private runNextLine(turnNo: number): void {
    this.lastStep = { kind: "NEXT_LINE", turnNo };
    this.update({ status: "NEXT_LINE", failReason: null });
    this.api.requestNextLine(turnNo).then(
      () => {
        if (!this.disposed) this.pollNextLine(turnNo);
      },
      () => {
        if (!this.disposed) this.fail("NETWORK");
      },
    );
  }

  private pollNextLine(turnNo: number): void {
    this.startJobPolling(
      () => this.api.getNextLine(turnNo),
      (response) => {
        if (response.status === "FAILED") {
          this.fail("SERVER_FAILED");
          return;
        }
        this.enterTurn(
          turnNo,
          response.sceneCue ?? "",
          response.opponentLine ?? "",
          response.actionPrompt ?? "",
          response.generationMode,
        );
      },
    );
  }

  private startJobPolling<T extends TurnEvaluationResponse | NextLineResponse>(
    fetch: () => Promise<T>,
    onTerminal: (response: T) => void,
  ): void {
    const polling = startPolling<T>({
      fetch,
      isTerminal: (response) => response.status !== "PENDING",
      intervalMs: this.pollIntervalMs,
      timeoutMs: this.pollTimeoutMs,
      maxConsecutiveErrors: this.maxConsecutivePollErrors,
    });
    this.polling = polling;
    void polling.promise.then((result) => {
      if (this.disposed) return;
      this.polling = null;
      switch (result.kind) {
        case "TERMINAL":
          onTerminal(result.value);
          break;
        case "TIMEOUT":
          this.fail("TIMEOUT");
          break;
        case "NETWORK":
          this.fail("NETWORK");
          break;
        case "CANCELLED":
          break;
      }
    });
  }

  private enterTurn(
    turnNo: number,
    sceneCue: string,
    opponentLine: string,
    actionPrompt: string,
    generationMode: SimulationFlowSnapshot["generationMode"],
  ): void {
    this.update({
      status: "ANSWERING",
      currentTurn: turnNo,
      sceneCue,
      opponentLine,
      actionPrompt,
      generationMode,
      transcript: null,
      evaluation: null,
      failReason: null,
    });
  }

  private fail(reason: SimulationFlowFailReason): void {
    this.update({ status: "FAILED", failReason: reason });
  }

  private update(patch: Partial<SimulationFlowSnapshot>): void {
    if (patch.status !== undefined) this.status = patch.status;
    if (patch.currentTurn !== undefined) this.currentTurn = patch.currentTurn;
    if (patch.maxTurn !== undefined) this.maxTurn = patch.maxTurn;
    if (patch.sceneCue !== undefined) this.sceneCue = patch.sceneCue;
    if (patch.opponentLine !== undefined)
      this.opponentLine = patch.opponentLine;
    if (patch.actionPrompt !== undefined)
      this.actionPrompt = patch.actionPrompt;
    if (patch.generationMode !== undefined)
      this.generationMode = patch.generationMode;
    if (patch.transcript !== undefined) this.transcript = patch.transcript;
    if (patch.evaluation !== undefined) this.evaluation = patch.evaluation;
    if (patch.failReason !== undefined) this.failReason = patch.failReason;
    this.notify();
  }

  private notify(): void {
    if (this.disposed) return;
    this.onChange({
      status: this.status,
      currentTurn: this.currentTurn,
      maxTurn: this.maxTurn,
      sceneCue: this.sceneCue,
      opponentLine: this.opponentLine,
      actionPrompt: this.actionPrompt,
      generationMode: this.generationMode,
      transcript: this.transcript,
      evaluation: this.evaluation ? { ...this.evaluation } : null,
      failReason: this.failReason,
    });
  }
}
