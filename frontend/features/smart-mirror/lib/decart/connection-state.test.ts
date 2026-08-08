import { describe, expect, it } from "vitest";

import { mapDecartConnectionState } from "./connection-state";

describe("mapDecartConnectionState", () => {
  it("connected/generating은 CONNECTED — 변환 프리뷰가 살아 있는 상태", () => {
    expect(mapDecartConnectionState("connected")).toBe("CONNECTED");
    expect(mapDecartConnectionState("generating")).toBe("CONNECTED");
  });

  it("connecting/reconnecting은 CONNECTING — 준비 문구를 보여주는 상태", () => {
    expect(mapDecartConnectionState("connecting")).toBe("CONNECTING");
    expect(mapDecartConnectionState("reconnecting")).toBe("CONNECTING");
  });

  it("disconnected는 CLOSED", () => {
    expect(mapDecartConnectionState("disconnected")).toBe("CLOSED");
  });
});
