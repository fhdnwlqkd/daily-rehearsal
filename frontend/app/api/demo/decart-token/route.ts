import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import {
  DEMO_AUTH_COOKIE,
  verifyDemoSessionToken,
} from "@/features/demo-rehearsal/server/demo-auth";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export async function POST() {
  const cookieStore = await cookies();
  if (!verifyDemoSessionToken(cookieStore.get(DEMO_AUTH_COOKIE)?.value)) {
    return NextResponse.json(
      { message: "데모 인증이 필요합니다." },
      { status: 401 },
    );
  }

  if (process.env.DECART_ENABLED?.trim().toLowerCase() === "false") {
    return NextResponse.json(
      { message: "Decart가 비활성화된 배포입니다." },
      { status: 503 },
    );
  }

  const demoApiKey = process.env.DEMO_API_KEY?.trim();
  if (!demoApiKey) {
    return NextResponse.json(
      { message: "데모 API 인증 환경변수가 설정되지 않았습니다." },
      { status: 503 },
    );
  }

  const headers = new Headers({
    accept: "application/json",
    "x-demo-key": demoApiKey,
  });
  const apiKey = process.env.API_KEY?.trim();
  if (apiKey) headers.set("x-api-key", apiKey);

  try {
    const backendResponse = await fetch(
      `${API_BASE_URL}/api/v1/demo/decart-token`,
      {
        method: "POST",
        headers,
        cache: "no-store",
      },
    );

    return new Response(backendResponse.body, {
      status: backendResponse.status,
      headers: {
        "cache-control": "no-store",
        "content-type":
          backendResponse.headers.get("content-type") ?? "application/json",
      },
    });
  } catch (error) {
    console.error("Failed to proxy demo Decart token request:", error);
    return NextResponse.json(
      { message: "데모 토큰 서버에 연결할 수 없습니다." },
      { status: 502 },
    );
  }
}
