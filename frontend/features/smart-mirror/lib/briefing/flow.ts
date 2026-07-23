import type {
  BriefingFlowFailReason,
  BriefingFlowSnapshot,
  BriefingFlowStatus,
  ContextStatus,
  SessionContextResponse,
  SubmitTranscriptResponse,
} from "../../types";
import { startPolling } from "../polling/poller";
import type { PollingHandle } from "../polling/poller";
import {
  CONTEXT_POLL_INTERVAL_MS,
  CONTEXT_POLL_MAX_CONSECUTIVE_ERRORS,
  CONTEXT_POLL_TIMEOUT_MS,
} from "./constants";

/**
 * 컨트롤러가 의존하는 API 표면. 실제로는 apis.ts 함수를 sessionId로 바인딩해
 * 주입하고, 테스트는 FakeApi를 주입한다 — 재질문 경로는 실서버가 아직 밟게
 * 해주지 않으므로(즉시 COMPLETED quirk) 이 주입 지점이 검증의 핵심이다.
 */
export interface BriefingFlowApi {
  submitBriefing: (transcript: string) => Promise<SubmitTranscriptResponse>;
  submitFollowUp: (transcript: string) => Promise<SubmitTranscriptResponse>;
  getContext: () => Promise<SessionContextResponse>;
}

export interface BriefingFlowControllerOptions {
  api: BriefingFlowApi;
  onChange: (snapshot: BriefingFlowSnapshot) => void;
  pollIntervalMs?: number;
  pollTimeoutMs?: number;
  maxConsecutivePollErrors?: number;
}

/** 폴링을 끝내는 서버 상태 — 나머지(NOT_STARTED·EXTRACTING·MERGING)는 계속 조회. */
const TERMINAL_CONTEXT_STATUSES: ReadonlySet<ContextStatus> = new Set([
  "FOLLOW_UP_REQUIRED",
  "COMPLETED",
  "FAILED",
]);

type SubmitRoute = "BRIEFING" | "FOLLOW_UP";

/**
 * 브리핑 스테이지의 제출→폴링→재질문 흐름 상태머신 (React 무관 순수 로직 —
 * SttController와 같은 구조: 컨트롤러가 상태를 소유하고 매 변화를 불변
 * 스냅샷으로 통지한다. 훅은 얇은 바인딩만 한다).
 *
 * 상태 전이는 BriefingFlowStatus 타입 주석 참고. 핵심 규칙:
 * - submitAnswer는 현재 상태로 밸브를 고른다: IDLE→briefing, FOLLOW_UP→follow-up.
 * - 재질문은 round 카운터일 뿐 별도 단계가 아니다. 라운드 상한도 두지 않는다 —
 *   max_attempt 소진 시 백엔드가 default를 채워 COMPLETED를 보장한다.
 * - FAILED에서 retry()는 마지막 transcript를 실패 시점의 밸브로 재전송한다.
 */
export class BriefingFlowController {
  private readonly api: BriefingFlowApi;
  private readonly onChange: (snapshot: BriefingFlowSnapshot) => void;
  private readonly pollIntervalMs: number;
  private readonly pollTimeoutMs: number;
  private readonly maxConsecutivePollErrors: number;

  private status: BriefingFlowStatus = "IDLE";
  private serverStatus: ContextStatus | null = null;
  private followUpQuestions: string[] = [];
  private round = 0;
  private failReason: BriefingFlowFailReason | null = null;

  /** retry용 — 마지막으로 전송(시도)한 답변과 그때의 밸브 */
  private lastTranscript: string | null = null;
  private lastRoute: SubmitRoute = "BRIEFING";

  private polling: PollingHandle<SessionContextResponse> | null = null;
  private disposed = false;

  constructor(options: BriefingFlowControllerOptions) {
    this.api = options.api;
    this.onChange = options.onChange;
    this.pollIntervalMs = options.pollIntervalMs ?? CONTEXT_POLL_INTERVAL_MS;
    this.pollTimeoutMs = options.pollTimeoutMs ?? CONTEXT_POLL_TIMEOUT_MS;
    this.maxConsecutivePollErrors =
      options.maxConsecutivePollErrors ?? CONTEXT_POLL_MAX_CONSECUTIVE_ERRORS;
  }

  /** IDLE→briefing POST, FOLLOW_UP→follow-up POST. 그 외 상태에선 무시(중복 제출 차단). */
  submitAnswer(transcript: string): void {
    if (this.disposed) return;
    if (this.status !== "IDLE" && this.status !== "FOLLOW_UP") return;
    this.send(this.status === "IDLE" ? "BRIEFING" : "FOLLOW_UP", transcript);
  }

  /** FAILED에서만 — 마지막 답변을 같은 밸브로 재전송한다. */
  retry(): void {
    if (this.disposed) return;
    if (this.status !== "FAILED" || this.lastTranscript === null) return;
    this.send(this.lastRoute, this.lastTranscript);
  }

  /** 타이머·폴링 해제. 이후 어떤 응답이 와도 통지하지 않는다. */
  dispose(): void {
    this.disposed = true;
    this.polling?.cancel();
    this.polling = null;
  }

  private send(route: SubmitRoute, transcript: string): void {
    this.lastRoute = route;
    this.lastTranscript = transcript;
    this.update({
      status: "SUBMITTING",
      serverStatus: null,
      followUpQuestions: [],
      failReason: null,
    });

    const post =
      route === "BRIEFING" ? this.api.submitBriefing : this.api.submitFollowUp;
    post(transcript).then(
      (response) => {
        if (this.disposed) return;
        this.update({ status: "PROCESSING", serverStatus: response.status });
        this.startContextPolling();
      },
      () => {
        if (this.disposed) return;
        this.fail("NETWORK");
      },
    );
  }

  private startContextPolling(): void {
    this.polling = startPolling<SessionContextResponse>({
      fetch: this.api.getContext,
      isTerminal: (response) => TERMINAL_CONTEXT_STATUSES.has(response.status),
      intervalMs: this.pollIntervalMs,
      timeoutMs: this.pollTimeoutMs,
      maxConsecutiveErrors: this.maxConsecutivePollErrors,
      // 비종결 응답(EXTRACTING/MERGING)도 문구 분기를 위해 반영한다
      onUpdate: (response) => {
        if (this.disposed || this.status !== "PROCESSING") return;
        if (!TERMINAL_CONTEXT_STATUSES.has(response.status)) {
          this.update({ serverStatus: response.status });
        }
      },
    });

    void this.polling.promise.then((result) => {
      if (this.disposed) return;
      this.polling = null;

      switch (result.kind) {
        case "TERMINAL":
          this.handleTerminalContext(result.value);
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

  private handleTerminalContext(response: SessionContextResponse): void {
    if (response.status === "COMPLETED") {
      this.update({ status: "COMPLETED", serverStatus: null });
      return;
    }
    if (response.status === "FOLLOW_UP_REQUIRED") {
      this.update({
        status: "FOLLOW_UP",
        serverStatus: null,
        followUpQuestions: response.followUpQuestions ?? [],
        round: this.round + 1,
      });
      return;
    }
    this.fail("SERVER_FAILED");
  }

  private fail(reason: BriefingFlowFailReason): void {
    this.update({ status: "FAILED", serverStatus: null, failReason: reason });
  }

  private update(
    patch: Partial<
      Pick<
        BriefingFlowSnapshot,
        "status" | "serverStatus" | "followUpQuestions" | "round" | "failReason"
      >
    >,
  ): void {
    if (patch.status !== undefined) this.status = patch.status;
    if (patch.serverStatus !== undefined)
      this.serverStatus = patch.serverStatus;
    if (patch.followUpQuestions !== undefined)
      this.followUpQuestions = patch.followUpQuestions;
    if (patch.round !== undefined) this.round = patch.round;
    if (patch.failReason !== undefined) this.failReason = patch.failReason;
    this.notify();
  }

  private notify(): void {
    if (this.disposed) return;
    this.onChange({
      status: this.status,
      serverStatus: this.serverStatus,
      followUpQuestions: [...this.followUpQuestions],
      round: this.round,
      failReason: this.failReason,
    });
  }
}
