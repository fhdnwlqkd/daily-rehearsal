"use client";

import { useCallback, useState } from "react";

import { confirmOutfit } from "../apis";
import type { ApiStatus } from "../types";

interface UseConfirmOutfitResult {
  status: ApiStatus;
  /** 팜홀드로 옷 확정 시 호출. 성공하면 세션이 REHEARSAL_READY로 전이된다. */
  confirm: (outfitId: string) => void;
}

/**
 * 옷 확정(PATCH /outfit)을 담당하는 훅. 사용자 액션으로 시작하므로 IDLE 출발.
 * READY는 종결 상태다 — 세션 상태가 이미 전이됐으므로 재확정은 없다.
 * ERROR는 재시도 허용(같은 제스처로 다시 확정).
 */
export function useConfirmOutfit(sessionId: string): UseConfirmOutfitResult {
  const [status, setStatus] = useState<ApiStatus>("IDLE");

  const confirm = useCallback(
    (outfitId: string) => {
      setStatus("LOADING");

      confirmOutfit(sessionId, outfitId)
        .then(() => {
          setStatus("READY");
        })
        .catch((error: unknown) => {
          console.error("Failed to confirm outfit:", error);
          setStatus("ERROR");
        });
    },
    [sessionId],
  );

  return { status, confirm };
}
