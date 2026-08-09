"use client";

import { useEffect, useState } from "react";

import { getBriefingContent } from "../apis";
import type { ApiStatus, BriefingContent } from "../types";

interface UseGetBriefingContentResult {
  /** READY 전에는 null. */
  content: BriefingContent | null;
  status: ApiStatus;
}

/**
 * 브리핑 화면에 타입별 고정 질문·예시 답변을 공급하는 훅.
 * 프록시(/api/backend)를 거쳐 실 API를 호출한다.
 */
export function useGetBriefingContent(
  situationType: string,
): UseGetBriefingContentResult {
  const [content, setContent] = useState<BriefingContent | null>(null);
  const [status, setStatus] = useState<ApiStatus>("LOADING");

  useEffect(() => {
    let cancelled = false;

    setStatus("LOADING");

    getBriefingContent(situationType)
      .then((response) => {
        if (cancelled) return;
        setContent(response);
        setStatus("READY");
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        console.error("Failed to load briefing content:", error);
        setStatus("ERROR");
      });

    return () => {
      cancelled = true;
    };
  }, [situationType]);

  return { content, status };
}
