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
} from "./constants";

/**
 * 컨트롤러가 의존하는 API 표면. 실제로는 apis.ts 함수를 sessionId로 바인딩해
 * 주입하고, 테스트는 FakeApi를 주입해 턴 진행·실패 조합을 임의로 밟는다.
 */
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
}

/** retry용 — 마지막으로 시도한 단계와 그 재료 */
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
 * - 턴은 스테이지 내부 상태다: 판정 실패는 같은 턴의 ANSWERING으로 돌아가고
 *   (재시도 무제한 — 2026-08-08 기획 확정), 성공만 턴을 전진시킨다.
 * - 종료 판정은 프론트 책임: currentTurn == maxTurn에서 성공하면 next-line을
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

  private status: SimulationFlowSnapshot["status"] = "STARTING";
  private currentTurn = 0;
  private maxTurn = 0;
  private opponentLine: string | null = null;
  private transcript: string | null = null;
  private evaluation: SimulationFeedback | null = null;
  private failReason: SimulationFlowFailReason | null = null;

  private lastStep: RetryStep = { kind: "START" };
  private begun = false;

  private polling: PollingHandle<
    TurnEvaluationResponse | NextLineResponse
  > | null = null;
  private disposed = false;

  constructor(options: SimulationFlowControllerOptions) {
    this.api = options.api;
    this.onChange = options.onChange;
    this.pollIntervalMs = options.pollIntervalMs ?? SIMULATION_POLL_INTERVAL_MS;
    this.pollTimeoutMs = options.pollTimeoutMs ?? SIMULATION_POLL_TIMEOUT_MS;
    this.maxConsecutivePollErrors =
      options.maxConsecutivePollErrors ??
      SIMULATION_POLL_MAX_CONSECUTIVE_ERRORS;
  }

  /** 시뮬레이션 시작(POST start). 한 번만 유효 — 서버도 세션당 한 번만 받는다. */
  begin(): void {
    if (this.disposed || this.begun) return;
    this.begun = true;
    this.runStart();
  }

  /** ANSWERING에서만 — 답변을 제출하고 판정 폴링으로 넘어간다. */
  submitAnswer(transcript: string): void {
    if (this.disposed || this.status !== "ANSWERING") return;
    this.runEvaluation(transcript);
  }

  /** FAILED에서만 — 마지막 단계를 같은 재료로 다시 밟는다. */
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

  /** 타이머·폴링 해제. 이후 어떤 응답이 와도 통지하지 않는다. */
  dispose(): void {
    this.disposed = true;
    this.polling?.cancel();
    this.polling = null;
  }

  private runStart(): void {
    this.lastStep = { kind: "START" };
    this.update({ status: "STARTING", failReason: null });

    this.api.start().then(
      (response) => {
        if (this.disposed) return;
        this.maxTurn = response.maxTurn;
        this.enterTurn(response.currentTurn, response.opponentLine);
      },
      () => {
        if (this.disposed) return;
        this.fail("NETWORK");
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
        if (this.disposed) return;
        this.pollEvaluation(turnNo);
      },
      () => {
        if (this.disposed) return;
        this.fail("NETWORK");
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
            success: false,
            feedback: EVALUATION_FALLBACK_FEEDBACK,
            fallback: true,
          });
          return;
        }
        this.handleEvaluationOutcome({
          success: response.success ?? false,
          feedback: response.feedback ?? "",
          fallback: response.fallback ?? false,
        });
      },
    );
  }

  private handleEvaluationOutcome(feedback: SimulationFeedback): void {
    if (!feedback.success) {
      // 같은 턴 재시도 — 상대 발화는 그대로 두고 피드백만 얹는다.
      this.update({ status: "ANSWERING", evaluation: feedback });
      return;
    }
    if (this.currentTurn >= this.maxTurn) {
      this.update({ status: "COMPLETED", evaluation: feedback });
      return;
    }
    this.update({ evaluation: feedback });
    this.runNextLine(this.currentTurn + 1);
  }

  private runNextLine(turnNo: number): void {
    this.lastStep = { kind: "NEXT_LINE", turnNo };
    this.update({ status: "NEXT_LINE", failReason: null });

    this.api.requestNextLine(turnNo).then(
      () => {
        if (this.disposed) return;
        this.pollNextLine(turnNo);
      },
      () => {
        if (this.disposed) return;
        this.fail("NETWORK");
      },
    );
  }

  private pollNextLine(turnNo: number): void {
    this.startJobPolling(
      () => this.api.getNextLine(turnNo),
      (response) => {
        if (response.status === "FAILED") {
          // AI 실패는 서버가 고정 발화로 흡수하므로 여기는 워커 장애뿐이다 —
          // retry가 next-line을 재요청하면 서버가 FAILED 턴을 재생성한다.
          this.fail("SERVER_FAILED");
          return;
        }
        this.enterTurn(turnNo, response.opponentLine ?? "");
      },
    );
  }

  /** 202 작업 공통 폴링 — PENDING이 아니게 되면 onTerminal로 넘긴다. */
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
          // dispose()에서만 발생 — 이미 통지 금지 상태다
          break;
      }
    });
  }

  /** 새 턴 진입 — 상대 발화를 갈아끼우고 직전 턴의 답변·피드백을 지운다. */
  private enterTurn(turnNo: number, opponentLine: string): void {
    this.update({
      status: "ANSWERING",
      currentTurn: turnNo,
      opponentLine,
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
    if (patch.opponentLine !== undefined)
      this.opponentLine = patch.opponentLine;
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
      opponentLine: this.opponentLine,
      transcript: this.transcript,
      evaluation: this.evaluation ? { ...this.evaluation } : null,
      failReason: this.failReason,
    });
  }
}
