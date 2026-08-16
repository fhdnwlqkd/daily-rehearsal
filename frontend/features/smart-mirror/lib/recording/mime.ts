/**
 * Chrome은 MP4/H.264를 isTypeSupported=true로 보고해도 WebRTC remote track
 * (Decart)을 실제 MediaRecorder에 연결하면 EncodingError(Internal Error)로
 * 중단되는 경우가 있다. 전시장 녹화의 안정성을 우선해 Chrome에서 가장 오래
 * 검증된 WebM/VP8을 먼저 쓰고, WebM을 지원하지 않는 환경에서만 MP4로 간다.
 */
export const RECORDING_MIME_CANDIDATES = [
  "video/webm;codecs=vp8",
  "video/webm",
  'video/mp4;codecs="avc1.42E01E,mp4a.40.2"',
  "video/mp4",
] as const;

/**
 * 지원되는 첫 후보를 고른다. isTypeSupported를 주입받는 이유:
 * MediaRecorder 전역이 없는 테스트 환경에서도 순수하게 검증하기 위해서다.
 */
export function pickRecordingMimeType(
  isTypeSupported: (mimeType: string) => boolean,
): string | null {
  return RECORDING_MIME_CANDIDATES.find(isTypeSupported) ?? null;
}
