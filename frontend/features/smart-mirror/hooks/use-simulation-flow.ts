"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import {
  getNextLine,
  getTurnEvaluation,
  requestNextLine,
  startSimulation,
  submitTurnEvaluation,
} from "../apis";
import { SimulationFlowController } from "../lib/simulation/flow";
import type { SimulationFlowSnapshot } from "../types";

const INITIAL_SNAPSHOT: SimulationFlowSnapshot = {
  status: "STARTING",
  currentTurn: 0,
  maxTurn: 0,
  opponentLine: null,
  transcript: null,
  evaluation: null,
  failReason: null,
};

export interface UseSimulationFlowResult extends SimulationFlowSnapshot {
  /** ANSWERING에서 답변 제출. 판정 폴링으로 넘어간다. */
  submitAnswer: (transcript: string) => void;
  /** FAILED에서 마지막 단계를 다시 밟는다. */
  retry: () => void;
}

/**
 * 시뮬레이션 시작→턴 진행(판정·다음 발화) 흐름 훅. 상태머신 본체는
 * SimulationFlowController(lib/simulation)에 있고, 이 훅은 React lifecycle
 * 연결과 apis.ts 함수의 sessionId 바인딩만 담당한다 (use-briefing-flow와
 * 같은 구조). 마운트 즉시 begin()으로 시뮬레이션을 시작한다 — 스테이지
 * 진입 = 시작이고, 서버도 세션당 한 번만 받는다.
 */
export function useSimulationFlow(sessionId: string): UseSimulationFlowResult {
  const [snapshot, setSnapshot] =
    useState<SimulationFlowSnapshot>(INITIAL_SNAPSHOT);
  const controllerRef = useRef<SimulationFlowController | null>(null);

  useEffect(() => {
    const controller = new SimulationFlowController({
      api: {
        start: () => startSimulation(sessionId),
        submitEvaluation: (turnNo, transcript) =>
          submitTurnEvaluation(sessionId, turnNo, transcript),
        getEvaluation: (turnNo) => getTurnEvaluation(sessionId, turnNo),
        requestNextLine: (turnNo) => requestNextLine(sessionId, turnNo),
        getNextLine: (turnNo) => getNextLine(sessionId, turnNo),
      },
      onChange: setSnapshot,
    });
    controllerRef.current = controller;
    controller.begin();

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
