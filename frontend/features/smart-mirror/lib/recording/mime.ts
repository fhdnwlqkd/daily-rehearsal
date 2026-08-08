/**
 * 코덱을 명시하지 않으면("video/mp4") 크롬이 오디오를 Opus-in-MP4로 녹음하는데,
 * 이 조합은 카카오톡·iOS 등 외부 플레이어/변환기가 못 읽어 소리가 사라진다.
 * H.264(avc1) + AAC(mp4a.40.2)를 명시해 어디서든 재생되는 표준 mp4를 우선한다
 * (#90 PoC 검증). webm은 mp4 미지원 브라우저용 마지막 보루다.
 */
export const RECORDING_MIME_CANDIDATES = [
  'video/mp4;codecs="avc1.42E01E,mp4a.40.2"',
  "video/mp4",
  "video/webm;codecs=vp8",
  "video/webm",
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
