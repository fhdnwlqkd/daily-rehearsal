"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { getSessionContext, submitBriefing, submitFollowUp } from "../apis";
import { BriefingFlowController } from "../lib/briefing/flow";
import type { BriefingFlowSnapshot } from "../types";

const INITIAL_SNAPSHOT: BriefingFlowSnapshot = {
  status: "IDLE",
  serverStatus: null,
  followUpQuestions: [],
  round: 0,
  failReason: null,
};

export interface UseBriefingFlowResult extends BriefingFlowSnapshot {
  /** 답변 제출. IDLE→briefing, FOLLOW_UP→follow-up으로 자동 라우팅된다. */
  submitAnswer: (transcript: string) => void;
  /** FAILED에서 마지막 답변을 재전송한다. */
  retry: () => void;
}

/**
 * 브리핑 제출→context 폴링→재질문 흐름 훅. 상태머신 본체는
 * BriefingFlowController(lib/briefing)에 있고, 이 훅은 React lifecycle 연결과
 * apis.ts 함수의 sessionId 바인딩만 담당한다 (use-speech-to-text와 같은 구조).
 * 재질문 경로까지 실서버 검증 완료(2026-07-23) — mock 전환점 불필요.
 */
export function useBriefingFlow(sessionId: string): UseBriefingFlowResult {
  const [snapshot, setSnapshot] =
    useState<BriefingFlowSnapshot>(INITIAL_SNAPSHOT);
  const controllerRef = useRef<BriefingFlowController | null>(null);

  useEffect(() => {
    const controller = new BriefingFlowController({
      api: {
        submitBriefing: (transcript) => submitBriefing(sessionId, transcript),
        submitFollowUp: (transcript) => submitFollowUp(sessionId, transcript),
        getContext: () => getSessionContext(sessionId),
      },
      onChange: setSnapshot,
    });
    controllerRef.current = controller;

    return () => {
      controller.dispose();
      controllerRef.current = null;
    };
  }, [sessionId]);

  const submitAnswer = useCallback((transcript: string) => {
    controllerRef.current?.submitAnswer(transcript);
  }, []);

  const retry = useCallback(() => {
    controllerRef.current?.retry();
  }, []);

  return { ...snapshot, submitAnswer, retry };
}
