"use client";

import { useEffect, useState } from "react";

import { getOutfits } from "../apis";
import type { ApiStatus, OutfitCandidate } from "../types";

interface UseGetOutfitsResult {
  /** READY 전에는 빈 배열. READY면 최소 1개 보장(0개는 백엔드가 404). */
  outfits: OutfitCandidate[];
  status: ApiStatus;
}

/**
 * 옷 입히기 화면에 옷 후보 목록을 공급하는 훅. 실 API 직결(이슈 #69 —
 * 백엔드 검증 완료 상태에서 시작해 mock 단계가 없다).
 * 404 C003은 "서버에 옷 미설정"이라는 구성 오류다 — 빈 목록이 아니라 ERROR.
 */
export function useGetOutfits(sessionId: string): UseGetOutfitsResult {
  const [outfits, setOutfits] = useState<OutfitCandidate[]>([]);
  const [status, setStatus] = useState<ApiStatus>("LOADING");

  useEffect(() => {
    let cancelled = false;

    setStatus("LOADING");

    getOutfits(sessionId)
      .then((response) => {
        if (cancelled) return;
        setOutfits(response);
        setStatus("READY");
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        console.error("Failed to load outfits:", error);
        setStatus("ERROR");
      });

    return () => {
      cancelled = true;
    };
  }, [sessionId]);

  return { outfits, status };
}
