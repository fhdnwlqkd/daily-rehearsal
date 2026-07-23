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
 *
 * mock/실API 전환점: 재질문 UI를 수동 확인할 때는 아래 api 객체를
 * data/mock-session-context.ts의 mock api로 갈아끼운다 — 실서버가 현재
 * 즉시 COMPLETED만 반환해서(백엔드 문의 중) 재질문 경로를 라이브로 못 밟는다.
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
