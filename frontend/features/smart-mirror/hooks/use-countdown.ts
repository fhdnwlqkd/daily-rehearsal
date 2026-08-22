"use client";

import { useEffect, useRef, useState } from "react";

import { startCountdown } from "../lib/timing/countdown";

interface UseCountdownArgs {
  durationSeconds: number;
  onExpire: () => void;
  enabled?: boolean;
}

/** 벽시계 기준 카운트다운. 탭이 느려져도 실제 제한시간이 늘어나지 않는다. */
export function useCountdown({
  durationSeconds,
  onExpire,
  enabled = true,
}: UseCountdownArgs): number {
  const [remainingSeconds, setRemainingSeconds] = useState(durationSeconds);
  const onExpireRef = useRef(onExpire);

  useEffect(() => {
    onExpireRef.current = onExpire;
  }, [onExpire]);

  useEffect(() => {
    if (!enabled) return;

    const countdown = startCountdown({
      durationSeconds,
      onTick: setRemainingSeconds,
      onExpire: () => onExpireRef.current(),
    });
    return countdown.cancel;
  }, [durationSeconds, enabled]);

  return remainingSeconds;
}
