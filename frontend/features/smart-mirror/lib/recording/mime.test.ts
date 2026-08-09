import { describe, expect, it } from "vitest";
import { pickRecordingMimeType, RECORDING_MIME_CANDIDATES } from "./mime";

describe("pickRecordingMimeType", () => {
  it("지원되는 첫 후보를 고른다 — mp4+명시 코덱이 최우선", () => {
    expect(pickRecordingMimeType(() => true)).toBe(
      RECORDING_MIME_CANDIDATES[0],
    );
  });

  it("mp4 계열이 모두 미지원이면 webm으로 내려간다", () => {
    const picked = pickRecordingMimeType(
      (mimeType) => !mimeType.startsWith("video/mp4"),
    );
    expect(picked).toBe("video/webm;codecs=vp8");
  });

  it("아무것도 지원하지 않으면 null — 소비처가 녹화를 건너뛴다", () => {
    expect(pickRecordingMimeType(() => false)).toBeNull();
  });
});
