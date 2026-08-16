const DEFAULT_OUTFIT_SELECTION_DURATION_SECONDS = 30;
const DEFAULT_SIMULATION_DURATION_SECONDS = 180;

export function resolveDurationSeconds(
  configuredValue: string | undefined,
  fallbackSeconds: number,
): number {
  if (configuredValue === undefined || configuredValue.trim() === "") {
    return fallbackSeconds;
  }
  const parsed = Number(configuredValue);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallbackSeconds;
}

export const OUTFIT_SELECTION_DURATION_SECONDS = resolveDurationSeconds(
  process.env.NEXT_PUBLIC_OUTFIT_SELECTION_DURATION_SECONDS,
  DEFAULT_OUTFIT_SELECTION_DURATION_SECONDS,
);

export const SIMULATION_DURATION_SECONDS = resolveDurationSeconds(
  process.env.NEXT_PUBLIC_SIMULATION_DURATION_SECONDS,
  DEFAULT_SIMULATION_DURATION_SECONDS,
);

/** 옷 선택부터 시뮬레이션 종료까지 허용하는 Decart 최대 연결 시간. */
export const MAX_DECART_CONNECTION_MS =
  (OUTFIT_SELECTION_DURATION_SECONDS + SIMULATION_DURATION_SECONDS) * 1000;
