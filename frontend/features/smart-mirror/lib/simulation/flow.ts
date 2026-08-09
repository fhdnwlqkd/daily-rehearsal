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

type RetryStep =
  | { kind: "START" }
  | { kind: "EVALUATION"; transcript: string }
  | { kind: "NEXT_LINE"; turnNo: number };

export class SimulationFlowController {
  private readonly api: SimulationFlowApi;
  private readonly onChange: (snapshot: SimulationFlowSnapshot) => void;
  private readonly pollIntervalMs: number;
  private readonly pollTimeoutMs: number;
  private readonly maxConsecutivePollErrors: number;

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
          this.fail("SERVER_FAILED");
          return;
        }
        this.handleEvaluationOutcome({
          outcome: response.outcome ?? "RETRY_REQUIRED",
          feedback: response.feedback ?? EVALUATION_FALLBACK_FEEDBACK,
          fallback: response.fallback ?? false,
        });
      },
    );
  }

  private handleEvaluationOutcome(feedback: SimulationFeedback): void {
    if (feedback.outcome === "RETRY_REQUIRED") {
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
