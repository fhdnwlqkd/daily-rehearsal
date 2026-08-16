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
const TURN_FEEDBACK_LINGER = 0;

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
          turnCompleted: false,
        });
      },
      getEvaluation: () => {
        const response = evaluations.shift();
        return response
          ? Promise.resolve(response)
          : Promise.reject(new Error("No evaluation response queued"));
      },
      requestNextLine: (turnNo: number) => {
        nextLineRequests.push(turnNo);
        return Promise.resolve({
          sessionId: "session-id",
          turnNo,
          status: "PENDING" as const,
        });
      },
      getNextLine: () => {
        const response = nextLines.shift();
        return response
          ? Promise.resolve(response)
          : Promise.reject(new Error("No next-line response queued"));
      },
    },
    evaluationSubmits,
    nextLineRequests,
    queueEvaluationResponse: (...responses: TurnEvaluationResponse[]) => {
      evaluations.push(...responses);
    },
    queueNextLineResponse: (...responses: NextLineResponse[]) => {
      nextLines.push(...responses);
    },
    queueEvaluation: (turnNo: number, outcome: TurnEvaluationOutcome) => {
      evaluations.push({
        sessionId: "session-id",
        turnNo,
        attemptNo: 1,
        status: "COMPLETED",
        outcome,
        feedback: `feedback-${outcome}`,
        fallback: false,
        turnCompleted: outcome !== "RETRY_REQUIRED",
      });
    },
    queueNextLine: (turnNo: number, generationMode: "NORMAL" | "RECOVERY") => {
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
    turnFeedbackLingerMs: TURN_FEEDBACK_LINGER,
  });
  return {
    ...fake,
    controller,
    snapshots,
    latest: () => {
      const snapshot = snapshots.at(-1);
      if (!snapshot) throw new Error("No simulation snapshot available");
      return snapshot;
    },
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

  it("holds the accepted-turn feedback on screen before requesting the next line", async () => {
    const fake = createFakeApi();
    const snapshots: SimulationFlowSnapshot[] = [];
    const lingerMs = 500;
    const controller = new SimulationFlowController({
      api: fake.api,
      onChange: (snapshot) => snapshots.push(snapshot),
      pollIntervalMs: POLL_INTERVAL,
      pollTimeoutMs: POLL_TIMEOUT,
      turnFeedbackLingerMs: lingerMs,
    });
    fake.queueEvaluation(1, "ACCEPTED");
    fake.queueNextLine(2, "NORMAL");

    controller.begin();
    await flush();
    controller.submitAnswer("안녕하세요. 반갑습니다.");
    await flush();

    // 피드백은 즉시 보이지만, 다음 발화는 아직 요청되지 않아야 한다.
    expect(snapshots.at(-1)).toMatchObject({
      status: "NEXT_LINE",
      currentTurn: 1,
      evaluation: { outcome: "ACCEPTED" },
    });
    expect(fake.nextLineRequests).toEqual([]);

    await vi.advanceTimersByTimeAsync(lingerMs);
    await flush();

    expect(fake.nextLineRequests).toEqual([2]);
    expect(snapshots.at(-1)).toMatchObject({
      status: "ANSWERING",
      currentTurn: 2,
      opponentLine: "line-2",
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

  it("absorbs an infrastructure evaluation failure as a retryable fallback", async () => {
    const fake = createFakeApi();
    fake.api.getEvaluation = () =>
      Promise.resolve({
        sessionId: "session-id",
        turnNo: 1,
        attemptNo: 1,
        status: "FAILED" as const,
        turnCompleted: false,
        failureReason: "database error",
      });
    const snapshots: SimulationFlowSnapshot[] = [];
    const controller = new SimulationFlowController({
      api: fake.api,
      onChange: (snapshot) => snapshots.push(snapshot),
      pollIntervalMs: POLL_INTERVAL,
      pollTimeoutMs: POLL_TIMEOUT,
      turnFeedbackLingerMs: TURN_FEEDBACK_LINGER,
    });

    controller.begin();
    await flush();
    controller.submitAnswer("answer");
    await flush();

    expect(snapshots.at(-1)).toMatchObject({
      status: "ANSWERING",
      currentTurn: 1,
      evaluation: {
        outcome: "RETRY_REQUIRED",
        fallback: true,
        turnCompleted: false,
      },
    });
  });

  it("retries a failed next-line worker request", async () => {
    const {
      controller,
      latest,
      queueEvaluation,
      queueNextLineResponse,
      nextLineRequests,
    } = setup();
    controller.begin();
    await flush();
    queueEvaluation(1, "ACCEPTED");
    queueNextLineResponse(
      {
        sessionId: "session-id",
        turnNo: 2,
        status: "FAILED",
        generationMode: "NORMAL",
        failureReason: "worker failed",
      },
      {
        sessionId: "session-id",
        turnNo: 2,
        status: "COMPLETED",
        generationMode: "NORMAL",
        sceneCue: "다시 이어지는 장면",
        opponentLine: "다시 질문할게요.",
        actionPrompt: "질문에 답해보세요.",
      },
    );

    controller.submitAnswer("첫 답변");
    await flush();
    await flush();
    expect(latest()).toMatchObject({
      status: "FAILED",
      failReason: "SERVER_FAILED",
    });

    controller.retry();
    await flush();
    await flush();
    expect(nextLineRequests).toEqual([2, 2]);
    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 2,
      opponentLine: "다시 질문할게요.",
    });
  });

  it("retries simulation start after a network failure", async () => {
    const fake = createFakeApi();
    const successfulStart = fake.api.start;
    let startCalls = 0;
    fake.api.start = () => {
      startCalls += 1;
      return startCalls === 1
        ? Promise.reject(new Error("network"))
        : successfulStart();
    };
    const snapshots: SimulationFlowSnapshot[] = [];
    const controller = new SimulationFlowController({
      api: fake.api,
      onChange: (snapshot) => snapshots.push(snapshot),
    });

    controller.begin();
    await flush();
    expect(snapshots.at(-1)).toMatchObject({
      status: "FAILED",
      failReason: "NETWORK",
    });

    controller.retry();
    await flush();
    expect(startCalls).toBe(2);
    expect(snapshots.at(-1)?.status).toBe("ANSWERING");
  });

  it("retries the same transcript after an evaluation submit failure", async () => {
    const fake = createFakeApi();
    const successfulSubmit = fake.api.submitEvaluation;
    let submitCalls = 0;
    fake.api.submitEvaluation = (turnNo, transcript) => {
      submitCalls += 1;
      return submitCalls === 1
        ? Promise.reject(new Error("network"))
        : successfulSubmit(turnNo, transcript);
    };
    fake.queueEvaluation(1, "RETRY_REQUIRED");
    const snapshots: SimulationFlowSnapshot[] = [];
    const controller = new SimulationFlowController({
      api: fake.api,
      onChange: (snapshot) => snapshots.push(snapshot),
      pollIntervalMs: POLL_INTERVAL,
      pollTimeoutMs: POLL_TIMEOUT,
      turnFeedbackLingerMs: TURN_FEEDBACK_LINGER,
    });

    controller.begin();
    await flush();
    controller.submitAnswer("전송할 답변");
    await flush();
    expect(snapshots.at(-1)?.status).toBe("FAILED");

    controller.retry();
    await flush();
    await flush();
    expect(submitCalls).toBe(2);
    expect(fake.evaluationSubmits).toEqual([
      { turnNo: 1, transcript: "전송할 답변" },
    ]);
  });

  it("fails when evaluation polling exceeds its timeout", async () => {
    const fake = createFakeApi();
    fake.api.getEvaluation = () =>
      Promise.resolve({
        sessionId: "session-id",
        turnNo: 1,
        attemptNo: 1,
        status: "PENDING" as const,
        turnCompleted: false,
      });
    const snapshots: SimulationFlowSnapshot[] = [];
    const controller = new SimulationFlowController({
      api: fake.api,
      onChange: (snapshot) => snapshots.push(snapshot),
      pollIntervalMs: POLL_INTERVAL,
      pollTimeoutMs: POLL_TIMEOUT,
      turnFeedbackLingerMs: TURN_FEEDBACK_LINGER,
    });

    controller.begin();
    await flush();
    controller.submitAnswer("답변");
    await vi.advanceTimersByTimeAsync(POLL_TIMEOUT + POLL_INTERVAL);

    expect(snapshots.at(-1)).toMatchObject({
      status: "FAILED",
      failReason: "TIMEOUT",
    });
  });

  it("ignores duplicate submissions while evaluating", async () => {
    const { controller, evaluationSubmits, queueEvaluationResponse } = setup();
    controller.begin();
    await flush();
    queueEvaluationResponse({
      sessionId: "session-id",
      turnNo: 1,
      attemptNo: 1,
      status: "PENDING",
      turnCompleted: false,
    });

    controller.submitAnswer("첫 제출");
    controller.submitAnswer("중복 제출");

    expect(evaluationSubmits).toEqual([{ turnNo: 1, transcript: "첫 제출" }]);
  });

  it("stops notifying after disposal", async () => {
    const { controller, snapshots, queueEvaluationResponse } = setup();
    controller.begin();
    await flush();
    queueEvaluationResponse({
      sessionId: "session-id",
      turnNo: 1,
      attemptNo: 1,
      status: "COMPLETED",
      outcome: "ACCEPTED",
      feedback: "좋아요.",
      fallback: false,
      turnCompleted: true,
    });

    controller.submitAnswer("답변");
    const notified = snapshots.length;
    controller.dispose();
    await vi.advanceTimersByTimeAsync(POLL_INTERVAL * 2);

    expect(snapshots).toHaveLength(notified);
  });
});
