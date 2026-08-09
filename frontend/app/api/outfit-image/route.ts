import type { NextRequest } from "next/server";

import { isAllowedImageUrl } from "@/features/smart-mirror/lib/decart/outfit-image";

/**
 * 옷 이미지 프록시. CloudFront가 CORS 헤더를 주지 않아 브라우저(Decart SDK의
 * setImage 포함)가 교차 출처로 이미지를 읽지 못한다 — 서버가 대신 받아
 * 같은 출처로 중계한다. 허용 목록(cloudfront.net) 밖 URL은 거부해
 * 열린 프록시(SSRF)가 되지 않게 한다.
 */
export async function GET(request: NextRequest) {
  const src = request.nextUrl.searchParams.get("src");
  if (!src || !isAllowedImageUrl(src)) {
    return new Response("Invalid image source", { status: 400 });
  }

  const response = await fetch(src, { cache: "no-store" });
  if (!response.ok) {
    return new Response("Upstream image fetch failed", {
      status: response.status,
    });
  }

  return new Response(response.body, {
    status: 200,
    headers: {
      "content-type": response.headers.get("content-type") ?? "image/png",
      // 옷 이미지는 전시 중 바뀌지 않는 정적 자산 — 브라우저 캐시로
      // 스와이프 왕복 시 재다운로드(2.6MB급)를 막는다.
      "cache-control": "public, max-age=3600",
    },
  });
}
