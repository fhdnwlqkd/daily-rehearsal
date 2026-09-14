import { afterAll, beforeEach, describe, expect, it } from "vitest";
import {
  createDemoSessionToken,
  DEMO_SESSION_TTL_SECONDS,
  isDemoAuthConfigured,
  verifyDemoPassword,
  verifyDemoSessionToken,
} from "./demo-auth";

const originalPassword = process.env.DEMO_PASSWORD;
const originalSessionSecret = process.env.DEMO_SESSION_SECRET;

describe("demo auth", () => {
  beforeEach(() => {
    process.env.DEMO_PASSWORD = "presentation-password";
    process.env.DEMO_SESSION_SECRET = "long-demo-session-secret";
  });

  afterAll(() => {
    process.env.DEMO_PASSWORD = originalPassword;
    process.env.DEMO_SESSION_SECRET = originalSessionSecret;
  });

  it("accepts only the configured password", () => {
    expect(isDemoAuthConfigured()).toBe(true);
    expect(verifyDemoPassword("presentation-password")).toBe(true);
    expect(verifyDemoPassword("wrong-password")).toBe(false);
  });

  it("signs a session that expires after four hours", () => {
    const issuedAt = 1_000;
    const token = createDemoSessionToken(issuedAt);

    expect(verifyDemoSessionToken(token, issuedAt)).toBe(true);
    expect(
      verifyDemoSessionToken(token, issuedAt + DEMO_SESSION_TTL_SECONDS),
    ).toBe(false);
  });

  it("rejects a modified session token", () => {
    const token = createDemoSessionToken(1_000);

    expect(verifyDemoSessionToken(`${token}modified`, 1_000)).toBe(false);
  });
});
