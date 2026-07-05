"use client";

import { useEffect, useState } from "react";

import { mockGetSituationTypesResponse } from "../data/mock-situation-types";
import type { ApiStatus, SituationType } from "../types";

// mock 단계: 응답 명세를 만족하는 mock을 실제 API처럼 비동기로 돌려준다.
// 실제 API가 붙으면 이 함수를 지우고 ../apis의 getSituationTypes를 쓴다.
function getSituationTypes() {
  return Promise.resolve(mockGetSituationTypesResponse);
}

interface UseGetSituationTypesResult {
  /** READY 전에는 빈 배열. */
  situationTypes: SituationType[];
  status: ApiStatus;
}

/**
 * 타입 선택 화면에 상황 타입 목록을 공급하는 훅.
 * 컴포넌트는 이 훅의 반환 형태만 알고, 데이터가 mock인지
 * 실제 API인지는 모른다 — mock/실API 전환은 이 파일 안에서만 일어난다.
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
