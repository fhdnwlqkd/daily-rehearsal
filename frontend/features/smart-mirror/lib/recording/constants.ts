/**
 * g2g 실측이 도착하기 전에 녹화가 시작될 때 쓰는 A/V 싱크 보정 기본값.
 * Decart 왕복+AI 추론 지연은 #90 PoC 실측에서 Δ≈0.6s였다 — 실측값이
 * 도착하면 DelayNode를 그 값으로 따라잡는다.
 */
export const DEFAULT_AV_SYNC_DELAY_MS = 600;

/** MediaRecorder 청크 간격. 긴 녹화에서 메모리를 한 덩어리로 쥐지 않게 한다. */
export const RECORDER_TIMESLICE_MS = 1000;
