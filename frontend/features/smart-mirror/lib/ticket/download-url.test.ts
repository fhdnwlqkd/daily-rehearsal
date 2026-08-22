import { describe, expect, it } from "vitest";
import { buildTicketDownloadUrl } from "./download-url";

describe("buildTicketDownloadUrl", () => {
  it("creates an absolute download URL from the current frontend origin", () => {
    expect(
      buildTicketDownloadUrl("https://daily-rehearsal.example", "session-id"),
    ).toBe("https://daily-rehearsal.example/download/session-id");
  });

  it("encodes unsafe session id characters as a single path segment", () => {
    expect(
      buildTicketDownloadUrl("https://daily-rehearsal.example", "a/b c"),
    ).toBe("https://daily-rehearsal.example/download/a%2Fb%20c");
  });

  it("rejects a non-web origin", () => {
    expect(() => buildTicketDownloadUrl("file:///tmp/rehearsal", "id")).toThrow(
      "http(s) origin",
    );
  });
});
