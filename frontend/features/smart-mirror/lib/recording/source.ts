import type { DecartConnectionStatus } from "../../types";

/**
 * DECART           변환 프리뷰 트랙으로 녹화 시작 ("내일의 모습"이 콘텐츠)
 * CAMERA_FALLBACK  변환 불가가 확정됨 — 원본 거울로 녹화 시작
 * WAIT             아직 시작하지 않는다 (다음 상태 변화에서 재판단)
 */
export type RecordingSourceDecision = "DECART" | "CAMERA_FALLBACK" | "WAIT";

/**
 * 녹화 소스 결정 — 한 테이크 원칙이라 시작 후엔 못 바꾸므로 성급히 잡으면
 * 안 된다. 핵심 규칙: 원본 폴백은 실패(ERROR)·종료(CLOSED)가 **확정**됐을
 * 때만이다. IDLE을 폴백으로 취급하면 안 된다 — 옷 입히기 진입 첫 렌더에는
 * 연결 시도 전이라 IDLE로 보이는데, 여기서 원본을 잡으면 몇 초 뒤 변환이
 * 연결돼도 생영상만 녹화된다 (2026-08-08 실검증에서 발견된 버그).
 */
export function decideRecordingSource(
  decartStatus: DecartConnectionStatus,
  decartTrackReady: boolean,
): RecordingSourceDecision {
  if (decartStatus === "CONNECTED" && decartTrackReady) return "DECART";
  if (decartStatus === "ERROR" || decartStatus === "CLOSED")
    return "CAMERA_FALLBACK";
  // IDLE(연결 전)·CONNECTING·CONNECTED인데 트랙 미도착 — 기다린다.
  return "WAIT";
}
