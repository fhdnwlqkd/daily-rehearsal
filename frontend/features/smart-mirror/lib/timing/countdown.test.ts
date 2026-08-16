import { afterEach, describe, expect, it, vi } from "vitest";

import { startCountdown } from "./countdown";

describe("startCountdown", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("expires exactly once at the wall-clock deadline", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(0);
    const ticks: number[] = [];
    const onExpire = vi.fn();

    startCountdown({
      durationSeconds: 3,
      onTick: (remaining) => ticks.push(remaining),
      onExpire,
    });

    expect(ticks.at(-1)).toBe(3);
    await vi.advanceTimersByTimeAsync(2000);
    expect(ticks.at(-1)).toBe(1);
    expect(onExpire).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1000);
    expect(ticks.at(-1)).toBe(0);
    expect(onExpire).toHaveBeenCalledTimes(1);

    await vi.advanceTimersByTimeAsync(5000);
    expect(onExpire).toHaveBeenCalledTimes(1);
  });

  it("stops without expiring when cancelled", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(0);
    const onExpire = vi.fn();
    const countdown = startCountdown({
      durationSeconds: 1,
      onTick: () => undefined,
      onExpire,
    });

    countdown.cancel();
    await vi.advanceTimersByTimeAsync(2000);

    expect(onExpire).not.toHaveBeenCalled();
  });
});
