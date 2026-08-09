import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
  NextLineResponse,
  SimulationFlowSnapshot,
  TurnEvaluationResponse,
} from "../../types";
import { SimulationFlowController } from "./flow";
import { EVALUATION_FALLBACK_FEEDBACK } from "./constants";

const POLL_INTERVAL = 1000;
const POLL_TIMEOUT = 10_000;

/**
 * POST/GET 응답을 수동으로 쥐고 있다가 순서대로 내보내는 API 대역.
 * 실서버는 판정 결과·발화를 마음대로 못 정하므로, 성공/실패/장애 조합은
 * 이 대역으로 결정적으로 밟는다 (briefing flow.test와 같은 방식).
 */
function createFakeApi() {
  const evaluationSubmits: Array<{ turnNo: number; transcript: string }> = [];
  const nextLineRequests: number[] = [];
  let startCalls = 0;
  let startRejects = false;
  let submitRejects = false;
  let nextLinePostRejects = false;
  const evaluationResponses: Array<TurnEvaluationResponse | Error> = [];
  const nextLineResponses: Array<NextLineResponse | Error> = [];

  return {
    api: {
      start: () => {
        startCalls += 1;
        if (startRejects) return Promise.reject(new Error("start 실패"));
        return Promise.resolve({
          sessionId: "s",
          currentTurn: 1,
          maxTurn: 2,
          opponentLine: "처음 뵙겠습니다.",
        });
      },
      submitEvaluation: (turnNo: number, transcript: string) => {
        evaluationSubmits.push({ turnNo, transcript });
        if (submitRejects) return Promise.reject(new Error("post 실패"));
        return Promise.resolve({
          sessionId: "s",
          turnNo,
          attemptNo: evaluationSubmits.length,
          status: "PENDING" as const,
          turnCompleted: false,
        });
      },
      getEvaluation: (turnNo: number) => {
        const next = evaluationResponses.shift();
        if (next === undefined)
          return Promise.reject(new Error(`판정 응답 미준비 (turn ${turnNo})`));
        if (next instanceof Error) return Promise.reject(next);
        return Promise.resolve(next);
      },
      requestNextLine: (turnNo: number) => {
        nextLineRequests.push(turnNo);
        if (nextLinePostRejects)
          return Promise.reject(new Error("next-line post 실패"));
        return Promise.resolve({
          sessionId: "s",
          turnNo,
          status: "PENDING" as const,
        });
      },
      getNextLine: (turnNo: number) => {
        const next = nextLineResponses.shift();
        if (next === undefined)
          return Promise.reject(new Error(`발화 응답 미준비 (turn ${turnNo})`));
        if (next instanceof Error) return Promise.reject(next);
        return Promise.resolve(next);
      },
    },
    evaluationSubmits,
    nextLineRequests,
    getStartCalls: () => startCalls,
    /** 다음 getEvaluation 호출들이 순서대로 돌려줄 응답을 쌓는다 */
    queueEvaluation: (...responses: Array<TurnEvaluationResponse | Error>) => {
      evaluationResponses.push(...responses);
    },
    queueNextLine: (...responses: Array<NextLineResponse | Error>) => {
      nextLineResponses.push(...responses);
    },
    rejectStart: () => {
      startRejects = true;
    },
    acceptStart: () => {
      startRejects = false;
    },
    rejectSubmit: () => {
      submitRejects = true;
    },
    rejectNextLinePost: () => {
      nextLinePostRejects = true;
    },
    acceptNextLinePost: () => {
      nextLinePostRejects = false;
    },
  };
}

function evaluationOf(
  extra: Partial<TurnEvaluationResponse> & { turnNo: number },
): TurnEvaluationResponse {
  return {
    sessionId: "s",
    attemptNo: 1,
    status: "COMPLETED",
    turnCompleted: extra.success ?? false,
    ...extra,
  };
}

function nextLineOf(
  extra: Partial<NextLineResponse> & { turnNo: number },
): NextLineResponse {
  return { sessionId: "s", status: "COMPLETED", ...extra };
}

function setup() {
  const fake = createFakeApi();
  const snapshots: SimulationFlowSnapshot[] = [];
  const controller = new SimulationFlowController({
    api: fake.api,
    onChange: (snapshot) => snapshots.push(snapshot),
    pollIntervalMs: POLL_INTERVAL,
    pollTimeoutMs: POLL_TIMEOUT,
    maxConsecutivePollErrors: 3,
  });
  const latest = (): SimulationFlowSnapshot => {
    const snapshot = snapshots[snapshots.length - 1];
    if (!snapshot) throw new Error("아직 통지된 snapshot이 없다");
    return snapshot;
  };
  return { ...fake, controller, snapshots, latest };
}

/** microtask(POST resolve → 폴링 첫 조회)까지 소화 */
async function flush() {
  await vi.advanceTimersByTimeAsync(0);
}

describe("SimulationFlowController", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("begin: start 응답의 턴 정보·첫 상대 발화로 ANSWERING에 진입한다", async () => {
    const { controller, latest } = setup();

    controller.begin();
    expect(latest().status).toBe("STARTING");

    await flush();
    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 1,
      maxTurn: 2,
      opponentLine: "처음 뵙겠습니다.",
      evaluation: null,
    });
  });

  it("정상 턴: 제출 → EVALUATING → 성공 판정 → NEXT_LINE → 다음 턴 ANSWERING", async () => {
    const {
      controller,
      latest,
      queueEvaluation,
      queueNextLine,
      nextLineRequests,
    } = setup();
    controller.begin();
    await flush();

    queueEvaluation(
      evaluationOf({ turnNo: 1, success: true, feedback: "좋았어요" }),
    );
    queueNextLine(nextLineOf({ turnNo: 2, opponentLine: "취미가 뭐예요?" }));

    controller.submitAnswer("반갑습니다");
    expect(latest().status).toBe("EVALUATING");
    expect(latest().transcript).toBe("반갑습니다");

    await flush(); // 판정 폴링 첫 조회 → 성공 → next-line POST
    expect(nextLineRequests).toEqual([2]);

    await flush(); // 발화 폴링 첫 조회 → COMPLETED
    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 2,
      opponentLine: "취미가 뭐예요?",
      transcript: null,
      evaluation: null, // 새 턴 진입 시 직전 피드백은 지운다
    });
  });

  it("판정 실패: 같은 턴 ANSWERING으로 돌아가고 상대 발화·피드백이 유지된다", async () => {
    const { controller, latest, queueEvaluation, evaluationSubmits } = setup();
    controller.begin();
    await flush();

    queueEvaluation(
      evaluationOf({ turnNo: 1, success: false, feedback: "조금 더 길게" }),
    );
    controller.submitAnswer("음");
    await flush();

    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 1,
      opponentLine: "처음 뵙겠습니다.",
      evaluation: { success: false, feedback: "조금 더 길게", fallback: false },
    });

    // 첫 실패 뒤 한 번은 같은 턴으로 다시 제출된다
    queueEvaluation(
      evaluationOf({ turnNo: 1, attemptNo: 2, success: true, feedback: "" }),
    );
    controller.submitAnswer("다시 답변");
    expect(evaluationSubmits.map((s) => s.turnNo)).toEqual([1, 1]);
  });

  it("두 번째 판정 실패: 실패 피드백을 유지하면서 다음 턴으로 넘어간다", async () => {
    const {
      controller,
      latest,
      queueEvaluation,
      queueNextLine,
      nextLineRequests,
    } = setup();
    controller.begin();
    await flush();

    queueEvaluation(
      evaluationOf({
        turnNo: 1,
        attemptNo: 2,
        success: false,
        feedback: "다음 단계로 넘어갈게요",
        turnCompleted: true,
      }),
    );
    queueNextLine(nextLineOf({ turnNo: 2, opponentLine: "다음 질문" }));

    controller.submitAnswer("두 번째 답변");
    await flush();
    expect(nextLineRequests).toEqual([2]);

    await flush();
    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 2,
      opponentLine: "다음 질문",
    });
  });

  it("판정 워커 장애(FAILED): flow 실패가 아니라 고정 피드백의 실패 판정으로 흡수한다", async () => {
    const { controller, latest, queueEvaluation } = setup();
    controller.begin();
    await flush();

    queueEvaluation(
      evaluationOf({ turnNo: 1, status: "FAILED", failureReason: "boom" }),
    );
    controller.submitAnswer("답변");
    await flush();

    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 1,
      evaluation: {
        success: false,
        feedback: EVALUATION_FALLBACK_FEEDBACK,
        fallback: true,
      },
    });
  });

  it("마지막 턴 성공: next-line을 요청하지 않고 COMPLETED로 끝난다", async () => {
    const {
      controller,
      latest,
      queueEvaluation,
      queueNextLine,
      nextLineRequests,
    } = setup();
    controller.begin();
    await flush();

    // 1턴 성공 → 2턴 진입 (maxTurn=2)
    queueEvaluation(evaluationOf({ turnNo: 1, success: true, feedback: "" }));
    queueNextLine(nextLineOf({ turnNo: 2, opponentLine: "두 번째 질문" }));
    controller.submitAnswer("첫 답변");
    await flush();
    await flush();
    expect(latest().currentTurn).toBe(2);

    // 2턴(마지막) 성공 → COMPLETED, next-line 추가 요청 없음
    queueEvaluation(
      evaluationOf({ turnNo: 2, success: true, feedback: "완벽해요" }),
    );
    controller.submitAnswer("마지막 답변");
    await flush();

    expect(latest().status).toBe("COMPLETED");
    expect(latest().evaluation?.feedback).toBe("완벽해요");
    expect(nextLineRequests).toEqual([2]);
  });

  it("next-line 워커 장애(FAILED): FAILED(SERVER_FAILED)로 끝나고 retry가 재요청한다", async () => {
    const {
      controller,
      latest,
      queueEvaluation,
      queueNextLine,
      nextLineRequests,
    } = setup();
    controller.begin();
    await flush();

    queueEvaluation(evaluationOf({ turnNo: 1, success: true, feedback: "" }));
    queueNextLine(
      nextLineOf({ turnNo: 2, status: "FAILED", failureReason: "boom" }),
    );
    controller.submitAnswer("답변");
    await flush();
    await flush();

    expect(latest()).toMatchObject({
      status: "FAILED",
      failReason: "SERVER_FAILED",
    });

    // retry → next-line POST부터 다시 (서버는 FAILED 턴을 재생성한다)
    queueNextLine(nextLineOf({ turnNo: 2, opponentLine: "다시 질문" }));
    controller.retry();
    await flush();

    expect(nextLineRequests).toEqual([2, 2]);
    expect(latest()).toMatchObject({
      status: "ANSWERING",
      currentTurn: 2,
      opponentLine: "다시 질문",
    });
  });

  it("start 실패: FAILED(NETWORK) 후 retry가 start를 다시 부른다", async () => {
    const { controller, latest, rejectStart, acceptStart, getStartCalls } =
      setup();
    rejectStart();

    controller.begin();
    await flush();
    expect(latest()).toMatchObject({ status: "FAILED", failReason: "NETWORK" });

    acceptStart();
    controller.retry();
    await flush();
    expect(getStartCalls()).toBe(2);
    expect(latest().status).toBe("ANSWERING");
  });

  it("제출 POST 실패: FAILED(NETWORK) 후 retry가 같은 transcript를 재전송한다", async () => {
    const {
      controller,
      latest,
      queueEvaluation,
      rejectSubmit,
      evaluationSubmits,
    } = setup();
    controller.begin();
    await flush();

    rejectSubmit();
    controller.submitAnswer("전송 실패할 답변");
    await flush();
    expect(latest()).toMatchObject({ status: "FAILED", failReason: "NETWORK" });

    // 재전송 경로 검증 — 대역을 복구시키는 스위치가 없으므로 호출 기록으로 확인
    queueEvaluation(evaluationOf({ turnNo: 1, success: true, feedback: "" }));
    controller.retry();
    expect(evaluationSubmits.map((s) => s.transcript)).toEqual([
      "전송 실패할 답변",
      "전송 실패할 답변",
    ]);
  });

  it("판정 폴링 타임아웃: FAILED(TIMEOUT)", async () => {
    const { controller, latest, queueEvaluation } = setup();
    controller.begin();
    await flush();

    // PENDING만 계속 — 데드라인까지 종결되지 않는다
    const pendingCount = POLL_TIMEOUT / POLL_INTERVAL + 2;
    queueEvaluation(
      ...Array.from({ length: pendingCount }, () =>
        evaluationOf({ turnNo: 1, status: "PENDING" }),
      ),
    );
    controller.submitAnswer("답변");
    await vi.advanceTimersByTimeAsync(POLL_TIMEOUT + POLL_INTERVAL);

    expect(latest()).toMatchObject({ status: "FAILED", failReason: "TIMEOUT" });
  });

  it("EVALUATING 중 재제출은 무시된다 (중복 제출 차단)", async () => {
    const { controller, queueEvaluation, evaluationSubmits } = setup();
    controller.begin();
    await flush();

    queueEvaluation(evaluationOf({ turnNo: 1, status: "PENDING" }));
    controller.submitAnswer("첫 제출");
    controller.submitAnswer("중복 제출");

    expect(evaluationSubmits).toHaveLength(1);
  });

  it("dispose 후에는 어떤 응답이 와도 통지하지 않는다", async () => {
    const { controller, snapshots, queueEvaluation } = setup();
    controller.begin();
    await flush();

    queueEvaluation(evaluationOf({ turnNo: 1, success: true, feedback: "" }));
    controller.submitAnswer("답변");
    const notified = snapshots.length;

    controller.dispose();
    await vi.advanceTimersByTimeAsync(POLL_INTERVAL * 3);

    expect(snapshots.length).toBe(notified);
  });
});
