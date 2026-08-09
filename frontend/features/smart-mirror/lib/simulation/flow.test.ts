import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
  NextLineResponse,
  SimulationFlowSnapshot,
  TurnEvaluationOutcome,
  TurnEvaluationResponse,
} from "../../types";
import { SimulationFlowController } from "./flow";

const POLL_INTERVAL = 1000;
const POLL_TIMEOUT = 10_000;

function createFakeApi() {
  const evaluations: TurnEvaluationResponse[] = [];
  const nextLines: NextLineResponse[] = [];
  const evaluationSubmits: Array<{ turnNo: number; transcript: string }> = [];
  const nextLineRequests: number[] = [];

  return {
    api: {
      start: () =>
        Promise.resolve({
          sessionId: "session-id",
          currentTurn: 1,
          maxTurn: 3,
          generationMode: "STATIC" as const,
          sceneCue: "상대방이 도착했습니다.",
          opponentLine: "안녕하세요.",
          actionPrompt: "자연스럽게 인사해보세요.",
        }),
      submitEvaluation: (turnNo: number, transcript: string) => {
        evaluationSubmits.push({ turnNo, transcript });
        return Promise.resolve({
          sessionId: "session-id",
          turnNo,
          attemptNo: evaluationSubmits.length,
          status: "PENDING" as const,
        });
      },
      getEvaluation: () => Promise.resolve(evaluations.shift()!),
      requestNextLine: (turnNo: number) => {
        nextLineRequests.push(turnNo);
        return Promise.resolve({
          sessionId: "session-id",
          turnNo,
          status: "PENDING" as const,
        });
      },
      getNextLine: () => Promise.resolve(nextLines.shift()!),
    },
    evaluationSubmits,
    nextLineRequests,
    queueEvaluation(turnNo: number, outcome: TurnEvaluationOutcome) {
      evaluations.push({
        sessionId: "session-id",
        turnNo,
        attemptNo: 1,
        status: "COMPLETED",
        outcome,
        feedback: `feedback-${outcome}`,
        fallback: false,
      });
    },
    queueNextLine(turnNo: number, generationMode: "NORMAL" | "RECOVERY") {
      nextLines.push({
        sessionId: "session-id",
        turnNo,
        status: "COMPLETED",
        generationMode,
        sceneCue: `scene-${turnNo}`,
        opponentLine: `line-${turnNo}`,
        actionPrompt: `action-${turnNo}`,
      });
    },
  };
}

function setup() {
  const fake = createFakeApi();
  const snapshots: SimulationFlowSnapshot[] = [];
  const controller = new SimulationFlowController({
    api: fake.api,
    onChange: (snapshot) => snapshots.push(snapshot),
    pollIntervalMs: POLL_INTERVAL,
    pollTimeoutMs: POLL_TIMEOUT,
  });
  return {
    ...fake,
    controller,
    latest: () => snapshots.at(-1)!,
  };
}

async function flush() {
  await vi.advanceTimersByTimeAsync(0);
}

describe("SimulationFlowController", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("starts with the complete static first-turn plan", async () => {
    const { controller, latest } = setup();
    controller.begin();
    await flush();

    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 1,
      generationMode: "STATIC",
      sceneCue: "상대방이 도착했습니다.",
      opponentLine: "안녕하세요.",
      actionPrompt: "자연스럽게 인사해보세요.",
    });
  });

  it("moves to a generated next turn after an accepted answer", async () => {
    const { controller, latest, queueEvaluation, queueNextLine } = setup();
    controller.begin();
    await flush();
    queueEvaluation(1, "ACCEPTED");
    queueNextLine(2, "NORMAL");

    controller.submitAnswer("안녕하세요. 반갑습니다.");
    await flush();
    await flush();

    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 2,
      generationMode: "NORMAL",
      sceneCue: "scene-2",
      opponentLine: "line-2",
      actionPrompt: "action-2",
    });
  });

  it("keeps the same turn when evaluation requires a retry", async () => {
    const { controller, latest, queueEvaluation } = setup();
    controller.begin();
    await flush();
    queueEvaluation(1, "RETRY_REQUIRED");

    controller.submitAnswer("이거 어떻게 하는 거지?");
    await flush();

    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 1,
      evaluation: { outcome: "RETRY_REQUIRED" },
    });
  });

  it("advances after a forced outcome and accepts a recovery plan", async () => {
    const { controller, latest, queueEvaluation, queueNextLine } = setup();
    controller.begin();
    await flush();
    queueEvaluation(1, "FORCED_ADVANCE");
    queueNextLine(2, "RECOVERY");

    controller.submitAnswer("잘 모르겠어요.");
    await flush();
    await flush();

    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 2,
      generationMode: "RECOVERY",
    });
  });

  it("completes after the third accepted turn", async () => {
    const { controller, latest, queueEvaluation, queueNextLine } = setup();
    controller.begin();
    await flush();

    queueEvaluation(1, "ACCEPTED");
    queueNextLine(2, "NORMAL");
    controller.submitAnswer("turn 1");
    await flush();
    await flush();

    queueEvaluation(2, "ACCEPTED");
    queueNextLine(3, "NORMAL");
    controller.submitAnswer("turn 2");
    await flush();
    await flush();

    queueEvaluation(3, "ACCEPTED");
    controller.submitAnswer("turn 3");
    await flush();

    expect(latest()).toMatchObject({ status: "COMPLETED", currentTurn: 3 });
  });

  it("treats an infrastructure evaluation failure as a failed flow", async () => {
    const fake = createFakeApi();
    fake.api.getEvaluation = () =>
      Promise.resolve({
        sessionId: "session-id",
        turnNo: 1,
        attemptNo: 1,
        status: "FAILED" as const,
        failureReason: "database error",
      });
    const snapshots: SimulationFlowSnapshot[] = [];
    const controller = new SimulationFlowController({
      api: fake.api,
      onChange: (snapshot) => snapshots.push(snapshot),
      pollIntervalMs: POLL_INTERVAL,
      pollTimeoutMs: POLL_TIMEOUT,
    });

    controller.begin();
    await flush();
    controller.submitAnswer("answer");
    await flush();

    expect(snapshots.at(-1)).toMatchObject({
      status: "FAILED",
      failReason: "SERVER_FAILED",
    });
  });
});
