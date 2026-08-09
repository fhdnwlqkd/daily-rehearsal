/**
 * 202 + 폴링 패턴의 범용 프리미티브. 브리핑 context 조회가 첫 소비자고,
 * 시뮬레이션(턴 평가·다음 발화 조회)도 같은 모양이라 재사용을 전제로 분리했다.
 *
 * - setInterval이 아니라 "응답 수신 후 다음 예약" setTimeout 체인 — 응답이
 *   간격보다 느려도 요청이 겹치지 않는다.
 * - 일시적 네트워크 에러는 maxConsecutiveErrors까지 허용하고 성공 시 리셋한다.
 * - cancel() 후 도착하는 늦은 응답은 settled 플래그로 무시한다.
 */

export interface StartPollingOptions<T> {
  fetch: () => Promise<T>;
  /** true를 반환하면 폴링을 끝내고 TERMINAL로 settle한다. */
  isTerminal: (value: T) => boolean;
  intervalMs: number;
  /** 첫 fetch 시작부터의 데드라인. 초과 시 TIMEOUT. */
  timeoutMs: number;
  /** 허용 연속 실패 횟수 (기본 3). 소진 시 NETWORK. */
  maxConsecutiveErrors?: number;
  /** 비종결 응답 포함 매 성공 응답마다 통지 — 진행 문구 분기용. */
  onUpdate?: (value: T) => void;
}

export type PollResult<T> =
  | { kind: "TERMINAL"; value: T }
  | { kind: "TIMEOUT" }
  | { kind: "NETWORK" }
  | { kind: "CANCELLED" };

export interface PollingHandle<T> {
  /** 항상 settle된다 (cancel 포함) — reject하지 않는다. */
  promise: Promise<PollResult<T>>;
  cancel: () => void;
}

export function startPolling<T>(
  options: StartPollingOptions<T>,
): PollingHandle<T> {
  const {
    fetch,
    isTerminal,
    intervalMs,
    timeoutMs,
    maxConsecutiveErrors = 3,
    onUpdate,
  } = options;

  let settled = false;
  let nextTimer: ReturnType<typeof setTimeout> | null = null;
  let consecutiveErrors = 0;

  let resolvePromise: (result: PollResult<T>) => void;
  const promise = new Promise<PollResult<T>>((resolve) => {
    resolvePromise = resolve;
  });

  const deadlineTimer = setTimeout(() => {
    settle({ kind: "TIMEOUT" });
  }, timeoutMs);

  function settle(result: PollResult<T>) {
    if (settled) return;
    settled = true;
    if (nextTimer !== null) clearTimeout(nextTimer);
    clearTimeout(deadlineTimer);
    resolvePromise(result);
  }

  async function tick() {
    nextTimer = null;
    try {
      const value = await fetch();
      if (settled) return;
      consecutiveErrors = 0;
      onUpdate?.(value);
      if (isTerminal(value)) {
        settle({ kind: "TERMINAL", value });
        return;
      }
    } catch {
      if (settled) return;
      consecutiveErrors += 1;
      if (consecutiveErrors >= maxConsecutiveErrors) {
        settle({ kind: "NETWORK" });
        return;
      }
    }
    nextTimer = setTimeout(() => void tick(), intervalMs);
  }

  // 첫 조회는 즉시 — 202 직후 이미 종결돼 있으면 한 번에 끝난다.
  void tick();

  return { promise, cancel: () => settle({ kind: "CANCELLED" }) };
}
