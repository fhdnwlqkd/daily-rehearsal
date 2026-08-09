"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { SttSnapshot } from "../types";
import {
  createBrowserRecognitionFactory,
  SttController,
} from "../lib/stt/recognition";

const INITIAL_SNAPSHOT: SttSnapshot = {
  status: "IDLE",
  transcript: "",
  errorType: null,
  failCount: 0,
};

export interface UseSpeechToTextOptions {
  /**
   * confirm()으로 발화가 확정될 때 최종 transcript를 받는다.
   * 백엔드로 나가는 건 이 문자열 하나뿐이다.
   */
  onConfirm: (transcript: string) => void;
}

export interface UseSpeechToTextResult extends SttSnapshot {
  /** IDLE → LISTENING. 언제 듣기 시작할지는 소비처가 정한다(TTS 재생 중 회피 등). */
  start: () => void;
  /** CANDIDATE의 후보 transcript를 확정하고 onConfirm을 발화한다. */
  confirm: () => void;
  /** 듣던 내용/후보를 폐기하고 IDLE로 돌아간다 (다시 말하기). */
  cancel: () => void;
  /** ERROR에서 재시도한다. failCount는 유지된다. */
  retry: () => void;
}

/**
 * Web Speech API 기반 STT 훅. briefing/follow-up/simulation이 같은
 * 인터페이스로 재사용한다. 상태머신 본체는 SttController(lib/stt)에 있고,
 * 이 훅은 React lifecycle 연결만 담당한다.
 *
 * confirm/cancel을 어떤 입력(제스처·키보드)에 묶을지는 소비처의 몫이다 —
 * 훅은 제스처를 모른다. 키보드 fallback UI도 소비처가 띄우고, 타이핑된
 * 텍스트를 onConfirm과 같은 경로로 합류시키면 된다.
 */
export function useSpeechToText(
  options: UseSpeechToTextOptions,
): UseSpeechToTextResult {
  const [snapshot, setSnapshot] = useState<SttSnapshot>(INITIAL_SNAPSHOT);
  const controllerRef = useRef<SttController | null>(null);

  // 최신 onConfirm을 ref로 들고 있어 stale closure 없이 호출한다
  const onConfirmRef = useRef(options.onConfirm);
  useEffect(() => {
    onConfirmRef.current = options.onConfirm;
  }, [options.onConfirm]);

  useEffect(() => {
    const controller = new SttController({
      createRecognition: createBrowserRecognitionFactory(),
      onChange: setSnapshot,
    });
    controllerRef.current = controller;

    return () => {
      controller.dispose();
      controllerRef.current = null;
    };
  }, []);

  const start = useCallback(() => {
    controllerRef.current?.start();
  }, []);

  const confirm = useCallback(() => {
    const transcript = controllerRef.current?.confirm() ?? null;
    if (transcript !== null) {
      onConfirmRef.current(transcript);
    }
  }, []);

  const cancel = useCallback(() => {
    controllerRef.current?.cancel();
  }, []);

  const retry = useCallback(() => {
    controllerRef.current?.retry();
  }, []);

  return { ...snapshot, start, confirm, cancel, retry };
}
