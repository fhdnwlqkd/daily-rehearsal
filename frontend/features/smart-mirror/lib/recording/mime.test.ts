import { describe, expect, it } from "vitest";
import { pickRecordingMimeType, RECORDING_MIME_CANDIDATES } from "./mime";

describe("pickRecordingMimeType", () => {
  it("지원되는 첫 후보를 고른다 — 안정적인 webm/vp8이 최우선", () => {
    expect(pickRecordingMimeType(() => true)).toBe(
      RECORDING_MIME_CANDIDATES[0],
    );
  });

  it("webm 계열이 모두 미지원이면 mp4로 내려간다", () => {
    const picked = pickRecordingMimeType(
      (mimeType) => !mimeType.startsWith("video/webm"),
    );
    expect(picked).toBe('video/mp4;codecs="avc1.42E01E,mp4a.40.2"');
  });

  it("아무것도 지원하지 않으면 null — 소비처가 녹화를 건너뛴다", () => {
    expect(pickRecordingMimeType(() => false)).toBeNull();
  });
});
