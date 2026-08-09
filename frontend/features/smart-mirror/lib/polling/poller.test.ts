import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { startPolling } from "./poller";
import type { PollResult } from "./poller";

interface FakeStatus {
  status: string;
}

/** fetch 호출마다 미리 넣어둔 응답을 순서대로 돌려주는 대역 */
function createFakeFetch(responses: Array<FakeStatus | Error>): {
  fetch: () => Promise<FakeStatus>;
  calls: () => number;
} {
  let index = 0;
  return {
    fetch: () => {
      const response = responses[Math.min(index, responses.length - 1)];
      index += 1;
      if (response === undefined)
        return Promise.reject(new Error("응답 미준비"));
      if (response instanceof Error) return Promise.reject(response);
      return Promise.resolve(response);
    },
    calls: () => index,
  };
}

const INTERVAL = 1000;
const TIMEOUT = 10_000;

function start(
  fetch: () => Promise<FakeStatus>,
  overrides: {
    onUpdate?: (value: FakeStatus) => void;
    maxConsecutiveErrors?: number;
  } = {},
) {
  return startPolling<FakeStatus>({
    fetch,
    isTerminal: (value) => value.status === "COMPLETED",
    intervalMs: INTERVAL,
    timeoutMs: TIMEOUT,
    ...overrides,
  });
}

/** settle 여부를 폴링하지 않고 확인하기 위한 상태 캡처 */
function capture<T>(promise: Promise<PollResult<T>>) {
  let result: PollResult<T> | null = null;
  void promise.then((r) => {
    result = r;
  });
  return () => result;
}

describe("startPolling", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("첫 응답이 종결이면 즉시 TERMINAL로 끝난다", async () => {
    const { fetch, calls } = createFakeFetch([{ status: "COMPLETED" }]);
    const handle = start(fetch);

    const result = await handle.promise;
    expect(result).toEqual({
      kind: "TERMINAL",
      value: { status: "COMPLETED" },
    });
    expect(calls()).toBe(1);
  });

  it("비종결 응답은 intervalMs 간격으로 다시 조회하고 onUpdate로 통지한다", async () => {
    const updates: string[] = [];
    const { fetch, calls } = createFakeFetch([
      { status: "EXTRACTING" },
      { status: "EXTRACTING" },
      { status: "COMPLETED" },
    ]);
    const handle = start(fetch, {
      onUpdate: (value) => updates.push(value.status),
    });

    await vi.advanceTimersByTimeAsync(0);
    expect(calls()).toBe(1);

    // 간격 직전에는 다음 조회가 없다
    await vi.advanceTimersByTimeAsync(INTERVAL - 1);
    expect(calls()).toBe(1);

    await vi.advanceTimersByTimeAsync(1);
    expect(calls()).toBe(2);

    await vi.advanceTimersByTimeAsync(INTERVAL);
    const result = await handle.promise;
    expect(result.kind).toBe("TERMINAL");
    expect(updates).toEqual(["EXTRACTING", "EXTRACTING", "COMPLETED"]);
  });

  it("데드라인 초과 시 TIMEOUT으로 끝나고 추가 조회를 멈춘다", async () => {
    const { fetch, calls } = createFakeFetch([{ status: "EXTRACTING" }]);
    const handle = start(fetch);

    await vi.advanceTimersByTimeAsync(TIMEOUT);
    const result = await handle.promise;
    expect(result.kind).toBe("TIMEOUT");

    const callsAtTimeout = calls();
    await vi.advanceTimersByTimeAsync(INTERVAL * 5);
    expect(calls()).toBe(callsAtTimeout);
  });

  it("일시 에러는 허용하고 성공하면 연속 실패 카운터가 리셋된다", async () => {
    // 에러 2회 → 성공 → 에러 2회 → 종결: maxConsecutiveErrors=3에 걸리지 않아야 한다
    const { fetch } = createFakeFetch([
      new Error("blip"),
      new Error("blip"),
      { status: "EXTRACTING" },
      new Error("blip"),
      new Error("blip"),
      { status: "COMPLETED" },
    ]);
    const handle = start(fetch);
    const getResult = capture(handle.promise);

    await vi.advanceTimersByTimeAsync(INTERVAL * 4);
    expect(getResult()).toBeNull();

    await vi.advanceTimersByTimeAsync(INTERVAL);
    const result = await handle.promise;
    expect(result.kind).toBe("TERMINAL");
  });

  it("연속 실패가 maxConsecutiveErrors에 도달하면 NETWORK로 끝난다", async () => {
    const { fetch } = createFakeFetch([new Error("down")]);
    const handle = start(fetch, { maxConsecutiveErrors: 3 });

    await vi.advanceTimersByTimeAsync(INTERVAL * 2);
    const result = await handle.promise;
    expect(result.kind).toBe("NETWORK");
  });

  it("cancel하면 CANCELLED로 settle되고 이후 조회가 없다", async () => {
    const { fetch, calls } = createFakeFetch([{ status: "EXTRACTING" }]);
    const handle = start(fetch);

    await vi.advanceTimersByTimeAsync(0);
    handle.cancel();

    const result = await handle.promise;
    expect(result.kind).toBe("CANCELLED");

    const callsAtCancel = calls();
    await vi.advanceTimersByTimeAsync(INTERVAL * 5);
    expect(calls()).toBe(callsAtCancel);
  });

  it("cancel 후 도착한 늦은 응답은 결과를 바꾸지 못한다", async () => {
    // resolve를 수동으로 쥐고 있는 fetch — in-flight 중에 cancel한다
    let resolveFetch: (value: FakeStatus) => void = () => {};
    const fetch = () =>
      new Promise<FakeStatus>((resolve) => {
        resolveFetch = resolve;
      });
    const handle = start(fetch);

    await vi.advanceTimersByTimeAsync(0);
    handle.cancel();
    resolveFetch({ status: "COMPLETED" });

    const result = await handle.promise;
    expect(result.kind).toBe("CANCELLED");
  });
});
