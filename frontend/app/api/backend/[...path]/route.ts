import type { NextRequest } from "next/server";

// 서버 전용 시크릿 — NEXT_PUBLIC_ 접두사를 붙이면 브라우저 번들에 노출되므로 금지.
const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";
const API_KEY = process.env.API_KEY;

/**
 * 백엔드 프록시. 브라우저는 같은 출처의 /api/backend/* 만 호출하고,
 * 실서버 주소·x-api-key는 여기(서버)에서만 붙는다 — 번들·네트워크 탭 어디에도
 * 시크릿이 노출되지 않고, 브라우저→백엔드 직접 호출이 아니라 CORS도 우회된다.
 */
async function proxy(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const { path } = await params;
  const url = `${API_BASE_URL}/${path.join("/")}${request.nextUrl.search}`;

  const headers = new Headers();
  const contentType = request.headers.get("content-type");
  if (contentType) headers.set("content-type", contentType);
  headers.set("accept", request.headers.get("accept") ?? "application/json");
  if (API_KEY) headers.set("x-api-key", API_KEY);

  const hasBody = request.method !== "GET" && request.method !== "HEAD";
  const response = await fetch(url, {
    method: request.method,
    headers,
    body: hasBody ? request.body : undefined,
    // Node fetch에서 스트림 body를 보낼 때 필수 옵션 (multipart 업로드 대비)
    ...(hasBody ? { duplex: "half" as const } : {}),
    cache: "no-store",
  });

  // body를 그대로 스트리밍 중계 — JSON은 물론 SSE·바이너리도 통과한다.
  return new Response(response.body, {
    status: response.status,
    headers: {
      "content-type":
        response.headers.get("content-type") ?? "application/json",
    },
  });
}

export {
  proxy as GET,
  proxy as POST,
  proxy as PUT,
  proxy as PATCH,
  proxy as DELETE,
};
