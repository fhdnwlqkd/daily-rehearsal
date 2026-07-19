"use client";

import { useEffect, useState } from "react";

import { getSituationTypes } from "../apis";
import type { ApiStatus, SituationType } from "../types";

interface UseGetSituationTypesResult {
  /** READY 전에는 빈 배열. */
  situationTypes: SituationType[];
  status: ApiStatus;
}

/**
 * 타입 선택 화면에 상황 타입 목록을 공급하는 훅.
 * 2026-07-19 실서버 전환 완료 — 프록시(/api/backend)를 거쳐 실 API를 호출한다.
 */
export function useGetSituationTypes(): UseGetSituationTypesResult {
  const [situationTypes, setSituationTypes] = useState<SituationType[]>([]);
  const [status, setStatus] = useState<ApiStatus>("LOADING");

  useEffect(() => {
    let cancelled = false;

    setStatus("LOADING");

    getSituationTypes()
      .then((response) => {
        if (cancelled) return;
        setSituationTypes(response);
        setStatus("READY");
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        console.error("Failed to load situation types:", error);
        setStatus("ERROR");
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return { situationTypes, status };
}
