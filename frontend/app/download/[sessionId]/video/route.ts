import type { NextRequest } from "next/server";

interface VideoStatusResponse {
  success: boolean;
  data?: {
    videoUrl: string | null;
    status: "NONE" | "PENDING" | "COMPLETED" | "FAILED";
  };
}

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";
const API_KEY = process.env.API_KEY;

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ sessionId: string }> },
) {
  const { sessionId } = await params;
  const headers = new Headers({ accept: "application/json" });
  if (API_KEY) headers.set("x-api-key", API_KEY);

  const statusResponse = await fetch(
    `${API_BASE_URL}/api/v1/sessions/${encodeURIComponent(sessionId)}/video`,
    { headers, cache: "no-store" },
  );
  if (!statusResponse.ok) {
    return new Response("영상 정보를 불러오지 못했습니다.", {
      status: statusResponse.status,
    });
  }

  const payload = (await statusResponse.json()) as VideoStatusResponse;
  const video = payload.data;
  if (!payload.success || video?.status !== "COMPLETED" || !video.videoUrl) {
    return new Response("영상이 아직 준비되지 않았습니다.", { status: 409 });
  }

  const videoUrl = new URL(video.videoUrl);
  if (videoUrl.protocol !== "http:" && videoUrl.protocol !== "https:") {
    return new Response("영상 주소가 올바르지 않습니다.", { status: 502 });
  }

  const videoResponse = await fetch(videoUrl, { cache: "no-store" });
  if (!videoResponse.ok || !videoResponse.body) {
    return new Response("영상을 내려받지 못했습니다.", { status: 502 });
  }

  const contentType =
    videoResponse.headers.get("content-type") ?? "application/octet-stream";
  const extension = contentType.includes("mp4") ? "mp4" : "webm";
  const responseHeaders = new Headers({
    "content-type": contentType,
    "content-disposition": `attachment; filename="daily-rehearsal.${extension}"`,
    "cache-control": "private, no-store",
  });
  const contentLength = videoResponse.headers.get("content-length");
  if (contentLength) responseHeaders.set("content-length", contentLength);

  return new Response(videoResponse.body, { headers: responseHeaders });
}
