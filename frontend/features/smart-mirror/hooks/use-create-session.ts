"use client";

import { useCallback, useState } from "react";

import { createSession } from "../apis";
import type { ApiStatus, CreateSessionResponse } from "../types";

interface UseCreateSessionResult {
  /** READY 전에는 null. sessionId는 이후 세션 API 경로에 사용한다. */
  session: CreateSessionResponse | null;
  status: ApiStatus;
  /** 제스처로 상황 타입 확정 시 호출. situationType은 situation-types의 식별자. */
  create: (situationType: string) => void;
}

/**
 * 상황 타입 확정 → 세션 생성을 담당하는 훅.
 * 마운트 시 자동 호출이 아니라 사용자 액션(create)으로 시작하므로
 * IDLE에서 출발한다. 2026-07-19 실서버 전환 완료 — 프록시를 거쳐 실 API를 호출한다.
 */
export function useCreateSession(): UseCreateSessionResult {
  const [session, setSession] = useState<CreateSessionResponse | null>(null);
  const [status, setStatus] = useState<ApiStatus>("IDLE");

  const create = useCallback((situationType: string) => {
    setStatus("LOADING");

    createSession(situationType)
      .then((response) => {
        setSession(response);
        setStatus("READY");
      })
      .catch((error: unknown) => {
        console.error("Failed to create session:", error);
        setSession(null);
        setStatus("ERROR");
      });
  }, []);

  return { session, status, create };
}
