import { describe, expect, it } from "vitest";
import { decideRecordingSource } from "./source";

describe("decideRecordingSource", () => {
  it("CONNECTED + 트랙 도착 → 변환 프리뷰로 녹화", () => {
    expect(decideRecordingSource("CONNECTED", true)).toBe("DECART");
  });

  it("CONNECTED인데 트랙 미도착 → 기다린다", () => {
    expect(decideRecordingSource("CONNECTED", false)).toBe("WAIT");
  });

  it("IDLE → 기다린다 — 진입 첫 렌더의 IDLE에서 원본을 잡으면 변환 연결 후에도 생영상만 녹화된다 (회귀 방지)", () => {
    expect(decideRecordingSource("IDLE", false)).toBe("WAIT");
  });

  it("CONNECTING → 기다린다", () => {
    expect(decideRecordingSource("CONNECTING", false)).toBe("WAIT");
  });

  it("ERROR/CLOSED 확정 시에만 원본 거울로 폴백한다", () => {
    expect(decideRecordingSource("ERROR", false)).toBe("CAMERA_FALLBACK");
    expect(decideRecordingSource("CLOSED", false)).toBe("CAMERA_FALLBACK");
  });
});
