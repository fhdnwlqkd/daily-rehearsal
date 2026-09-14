import { NextResponse } from "next/server";
import {
  createDemoSessionToken,
  DEMO_AUTH_COOKIE,
  DEMO_SESSION_TTL_SECONDS,
  isDemoAuthConfigured,
  verifyDemoPassword,
} from "@/features/demo-rehearsal/server/demo-auth";

type PasswordRequest = { password?: unknown };

export async function POST(request: Request) {
  if (!isDemoAuthConfigured()) {
    return NextResponse.json(
      { message: "데모 인증 환경변수가 설정되지 않았습니다." },
      { status: 503 },
    );
  }

  const body = (await request
    .json()
    .catch(() => null)) as PasswordRequest | null;
  if (
    typeof body?.password !== "string" ||
    !verifyDemoPassword(body.password)
  ) {
    return NextResponse.json(
      { message: "비밀번호가 올바르지 않습니다." },
      { status: 401 },
    );
  }

  const response = NextResponse.json({ authenticated: true });
  response.cookies.set(DEMO_AUTH_COOKIE, createDemoSessionToken(), {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    path: "/",
    maxAge: DEMO_SESSION_TTL_SECONDS,
  });
  response.headers.set("Cache-Control", "no-store");
  return response;
}
