export interface StartCountdownOptions {
  durationSeconds: number;
  onTick: (remainingSeconds: number) => void;
  onExpire: () => void;
  tickIntervalMs?: number;
  now?: () => number;
}

export interface CountdownHandle {
  cancel: () => void;
}

/** setInterval 지연과 무관하게 실제 마감시각을 기준으로 남은 초를 계산한다. */
export function startCountdown({
  durationSeconds,
  onTick,
  onExpire,
  tickIntervalMs = 200,
  now = Date.now,
}: StartCountdownOptions): CountdownHandle {
  const deadline = now() + durationSeconds * 1000;
  let expired = false;
  let interval: ReturnType<typeof setInterval> | null = null;

  function tick() {
    const remaining = Math.max(0, Math.ceil((deadline - now()) / 1000));
    onTick(remaining);
    if (remaining === 0 && !expired) {
      expired = true;
      onExpire();
      if (interval !== null) clearInterval(interval);
      interval = null;
    }
  }

  interval = setInterval(tick, tickIntervalMs);
  tick();

  return {
    cancel: () => {
      if (interval !== null) clearInterval(interval);
      interval = null;
    },
  };
}
