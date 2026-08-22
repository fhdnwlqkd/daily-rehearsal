import { describe, expect, it } from "vitest";

import { resolveDurationSeconds } from "./constants";

describe("resolveDurationSeconds", () => {
  it("uses a configured positive integer", () => {
    expect(resolveDurationSeconds("45", 30)).toBe(45);
  });

  it.each([undefined, "", "0", "-1", "1.5", "invalid"])(
    "falls back for an invalid duration: %s",
    (configured) => {
      expect(resolveDurationSeconds(configured, 30)).toBe(30);
    },
  );
});
