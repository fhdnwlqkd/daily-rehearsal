import { createHash, createHmac, timingSafeEqual } from "node:crypto";

export const DEMO_AUTH_COOKIE = "daily_rehearsal_demo_auth";
export const DEMO_SESSION_TTL_SECONDS = 4 * 60 * 60;

function configuredValue(name: "DEMO_PASSWORD" | "DEMO_SESSION_SECRET") {
  const value = process.env[name]?.trim();
  return value ? value : null;
}

function sha256(value: string) {
  return createHash("sha256").update(value).digest();
}

function signature(payload: string, secret: string) {
  return createHmac("sha256", secret).update(payload).digest("hex");
}

export function isDemoAuthConfigured() {
  return (
    configuredValue("DEMO_PASSWORD") !== null &&
    configuredValue("DEMO_SESSION_SECRET") !== null
  );
}

export function verifyDemoPassword(candidate: string) {
  const expected = configuredValue("DEMO_PASSWORD");
  return (
    expected !== null && timingSafeEqual(sha256(candidate), sha256(expected))
  );
}

export function createDemoSessionToken(
  nowSeconds = Math.floor(Date.now() / 1000),
) {
  const secret = configuredValue("DEMO_SESSION_SECRET");
  if (!secret) throw new Error("Demo session secret is not configured");

  const expiresAt = String(nowSeconds + DEMO_SESSION_TTL_SECONDS);
  return `${expiresAt}.${signature(expiresAt, secret)}`;
}

export function verifyDemoSessionToken(
  token: string | undefined,
  nowSeconds = Math.floor(Date.now() / 1000),
) {
  if (!token) return false;

  const secret = configuredValue("DEMO_SESSION_SECRET");
  if (!secret) return false;

  const [expiresAtText, providedSignature, extra] = token.split(".");
  if (!expiresAtText || !providedSignature || extra !== undefined) return false;

  const expiresAt = Number(expiresAtText);
  if (!Number.isInteger(expiresAt) || expiresAt <= nowSeconds) return false;

  const expectedSignature = signature(expiresAtText, secret);
  const provided = Buffer.from(providedSignature, "utf8");
  const expected = Buffer.from(expectedSignature, "utf8");
  return (
    provided.length === expected.length && timingSafeEqual(provided, expected)
  );
}
