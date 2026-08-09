import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
  BriefingFlowSnapshot,
  SessionContextResponse,
  SubmitTranscriptResponse,
} from "../../types";
import { BriefingFlowController } from "./flow";

const POLL_INTERVAL = 1000;
const POLL_TIMEOUT = 10_000;

/**
 * POST/GET 응답을 수동으로 쥐고 있다가 순서대로 내보내는 API 대역.
 * 실서버는 라운드 수·질문을 마음대로 못 정하므로, 재질문 다라운드·실패
 * 조합은 이 대역으로 결정적으로 밟는다.
 */
function createFakeApi() {
  const briefingCalls: string[] = [];
  const followUpCalls: string[] = [];
  let briefingResult: Promise<SubmitTranscriptResponse> = Promise.resolve({
    sessionId: "s",
    status: "EXTRACTING",
  });
  const followUpResult: Promise<SubmitTranscriptResponse> = Promise.resolve({
    sessionId: "s",
    status: "MERGING",
  });
  const contextResponses: Array<SessionContextResponse | Error> = [];

  return {
    api: {
      submitBriefing: (transcript: string) => {
        briefingCalls.push(transcript);
        return briefingResult;
      },
      submitFollowUp: (transcript: string) => {
        followUpCalls.push(transcript);
        return followUpResult;
      },
      getContext: () => {
        const next = contextResponses.shift();
        if (next === undefined)
          return Promise.reject(new Error("컨텍스트 응답 미준비"));
        if (next instanceof Error) return Promise.reject(next);
        return Promise.resolve(next);
      },
    },
    briefingCalls,
    followUpCalls,
    /** 다음 getContext 호출들이 순서대로 돌려줄 응답을 쌓는다 */
    queueContext: (...responses: Array<SessionContextResponse | Error>) => {
      contextResponses.push(...responses);
    },
    rejectBriefing: () => {
      briefingResult = Promise.reject(new Error("post 실패"));
    },
  };
}

function contextOf(
  status: SessionContextResponse["status"],
  extra: Partial<SessionContextResponse> = {},
): SessionContextResponse {
  return { sessionId: "s", status, ...extra };
}

function setup() {
  const fake = createFakeApi();
  const snapshots: BriefingFlowSnapshot[] = [];
  const controller = new BriefingFlowController({
    api: fake.api,
    onChange: (snapshot) => snapshots.push(snapshot),
    pollIntervalMs: POLL_INTERVAL,
    pollTimeoutMs: POLL_TIMEOUT,
    maxConsecutivePollErrors: 3,
  });
  const latest = (): BriefingFlowSnapshot => {
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

describe("BriefingFlowController", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("정상 경로: 제출 → SUBMITTING → PROCESSING → 폴링 COMPLETED → COMPLETED", async () => {
    const { controller, briefingCalls, queueContext, snapshots, latest } =
      setup();
    queueContext(contextOf("EXTRACTING"), contextOf("COMPLETED"));

    controller.submitAnswer("내일 소개팅이 있어요");
    expect(latest().status).toBe("SUBMITTING");

    await flush();
    expect(briefingCalls).toEqual(["내일 소개팅이 있어요"]);
    expect(
      snapshots.some(
        (s) => s.status === "PROCESSING" && s.serverStatus === "EXTRACTING",
      ),
    ).toBe(true);

    await vi.advanceTimersByTimeAsync(POLL_INTERVAL);
    expect(latest().status).toBe("COMPLETED");
    expect(latest().round).toBe(0);
  });

  it("재질문 경로: FOLLOW_UP_REQUIRED → FOLLOW_UP, 이때 제출은 follow-up 밸브로 간다", async () => {
    const { controller, briefingCalls, followUpCalls, queueContext, latest } =
      setup();
    queueContext(
      contextOf("FOLLOW_UP_REQUIRED", {
        missingSlotKeys: ["desired_persona"],
        followUpQuestions: ["어떤 모습으로 보이고 싶나요?"],
      }),
    );

    controller.submitAnswer("최초 답변");
    await flush();

    expect(latest().status).toBe("FOLLOW_UP");
    expect(latest().followUpQuestions).toEqual([
      "어떤 모습으로 보이고 싶나요?",
    ]);
    expect(latest().round).toBe(1);

    // FOLLOW_UP 상태의 제출은 briefing이 아니라 follow-up POST여야 한다
    queueContext(contextOf("MERGING"), contextOf("COMPLETED"));
    controller.submitAnswer("따뜻한 모습이요");
    await flush();

    expect(briefingCalls).toEqual(["최초 답변"]);
    expect(followUpCalls).toEqual(["따뜻한 모습이요"]);
    expect(latest().serverStatus).toBe("MERGING");

    await vi.advanceTimersByTimeAsync(POLL_INTERVAL);
    expect(latest().status).toBe("COMPLETED");
  });

  it("재질문 2라운드 반복 시 round가 증가한다", async () => {
    const { controller, queueContext, latest } = setup();

    queueContext(
      contextOf("FOLLOW_UP_REQUIRED", { followUpQuestions: ["q1"] }),
    );
    controller.submitAnswer("최초");
    await flush();
    expect(latest().round).toBe(1);

    queueContext(
      contextOf("FOLLOW_UP_REQUIRED", { followUpQuestions: ["q2"] }),
    );
    controller.submitAnswer("1라운드 답");
    await flush();
    expect(latest().status).toBe("FOLLOW_UP");
    expect(latest().followUpQuestions).toEqual(["q2"]);
    expect(latest().round).toBe(2);
  });

  it("서버 FAILED면 FAILED/SERVER_FAILED로 끝난다", async () => {
    const { controller, queueContext, latest } = setup();
    queueContext(contextOf("FAILED"));

    controller.submitAnswer("답변");
    await flush();

    expect(latest().status).toBe("FAILED");
    expect(latest().failReason).toBe("SERVER_FAILED");
  });

  it("폴링 데드라인 초과면 FAILED/TIMEOUT", async () => {
    const { controller, queueContext, latest } = setup();
    // 계속 비종결 — 데드라인까지 응답을 넉넉히 쌓는다
    queueContext(...Array.from({ length: 20 }, () => contextOf("EXTRACTING")));

    controller.submitAnswer("답변");
    await vi.advanceTimersByTimeAsync(POLL_TIMEOUT);

    expect(latest().status).toBe("FAILED");
    expect(latest().failReason).toBe("TIMEOUT");
  });

  it("POST 실패면 FAILED/NETWORK, retry는 같은 밸브(briefing)로 재전송한다", async () => {
    const { controller, briefingCalls, rejectBriefing, latest } = setup();

    rejectBriefing();
    controller.submitAnswer("답변");
    await flush();
    expect(latest().status).toBe("FAILED");
    expect(latest().failReason).toBe("NETWORK");

    // retry — briefing 밸브로 같은 transcript가 다시 나가야 한다
    // (briefingResult가 고정 reject라 성공까지는 안 가지만, 여기서 검증할 것은
    // 라우팅(호출 횟수·인자)뿐이다)
    controller.retry();
    expect(briefingCalls).toEqual(["답변", "답변"]);
  });

  it("폴링 연속 에러 소진이면 FAILED/NETWORK, retry는 follow-up 밸브를 기억한다", async () => {
    const { controller, followUpCalls, queueContext, latest } = setup();

    queueContext(contextOf("FOLLOW_UP_REQUIRED", { followUpQuestions: ["q"] }));
    controller.submitAnswer("최초");
    await flush();

    // follow-up 제출 후 폴링이 3연속 에러 → NETWORK
    controller.submitAnswer("추가 답변");
    await flush();
    await vi.advanceTimersByTimeAsync(POLL_INTERVAL * 3);
    expect(latest().status).toBe("FAILED");
    expect(latest().failReason).toBe("NETWORK");

    // retry는 briefing이 아니라 follow-up으로 재전송
    controller.retry();
    expect(followUpCalls).toEqual(["추가 답변", "추가 답변"]);
  });

  it("SUBMITTING·PROCESSING 중의 submitAnswer는 무시된다(중복 제출 차단)", async () => {
    const { controller, briefingCalls, followUpCalls, queueContext } = setup();
    queueContext(contextOf("EXTRACTING"), contextOf("COMPLETED"));

    controller.submitAnswer("첫 제출");
    controller.submitAnswer("중복 제출"); // SUBMITTING 중
    await flush();
    controller.submitAnswer("중복 제출2"); // PROCESSING 중

    expect(briefingCalls).toEqual(["첫 제출"]);
    expect(followUpCalls).toEqual([]);
  });

  it("dispose 후에는 폴링이 멈추고 어떤 통지도 오지 않는다", async () => {
    const { controller, queueContext, snapshots } = setup();
    queueContext(contextOf("EXTRACTING"), contextOf("COMPLETED"));

    controller.submitAnswer("답변");
    await flush();
    const countAtDispose = snapshots.length;

    controller.dispose();
    await vi.advanceTimersByTimeAsync(POLL_INTERVAL * 5);

    expect(snapshots.length).toBe(countAtDispose);
    expect(snapshots[snapshots.length - 1]?.status).not.toBe("COMPLETED");
  });

  it("NOT_STARTED·EXTRACTING·MERGING은 비종결로 계속 폴링한다", async () => {
    const { controller, queueContext, latest } = setup();
    queueContext(
      contextOf("NOT_STARTED"), // 202 직후 워커 픽업 전 레이스
      contextOf("EXTRACTING"),
      contextOf("MERGING"),
      contextOf("COMPLETED"),
    );

    controller.submitAnswer("답변");
    await flush();
    expect(latest().status).toBe("PROCESSING");

    await vi.advanceTimersByTimeAsync(POLL_INTERVAL * 3);
    expect(latest().status).toBe("COMPLETED");
  });
});
