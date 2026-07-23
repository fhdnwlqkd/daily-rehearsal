"use client";

import { useEffect, useRef, useState } from "react";
import {
  HAND_LOST_GRACE_MS,
  RUNTIME_ERROR_LIMIT,
} from "../lib/gesture/constants";
import { PalmHoldDetector } from "../lib/gesture/palm-hold-detector";
import { SwipeDetector } from "../lib/gesture/swipe-detector";
import type {
  GestureAction,
  GestureActionEvent,
  GestureEngineHandle,
  GestureEngineStatus,
} from "../types";

export interface UseGestureControllerOptions {
  /** 세션 루트의 useGestureEngine이 소유한 엔진 */
  engine: GestureEngineHandle;
  /** useCamera가 획득한 스트림. null이면 제스처만 휴면(에러 아님) */
  stream: MediaStream | null;
  /** NEXT/PREV/CONFIRM 발생 시 호출. 제스처든 키보드든 같은 콜백 */
  onAction: (event: GestureActionEvent) => void;
  /** false면 인식 루프/키 리스너 일시 정지 (기본 true) */
  enabled?: boolean;
}

export interface UseGestureControllerResult {
  /** 엔진 상태 중계 + 런타임 실패 반영. ERROR여도 키보드는 동작 */
  status: GestureEngineStatus;
  /** 손 감지 여부 — "손을 들어주세요" 힌트용 */
  handVisible: boolean;
  /** 0~1 팜홀드 진행률 — 차징 바 UI용 */
  confirmProgress: number;
}

const KEY_TO_ACTION: Record<string, GestureAction> = {
  ArrowRight: "NEXT",
  ArrowLeft: "PREV",
  Enter: "CONFIRM",
};

/**
 * 제스처 스테이지가 호출하는 소비 훅.
 * rAF 루프에서 프레임을 판별기에 통과시켜 이벤트를 만들고,
 * 키보드를 병렬 입력 소스로 병합한다. 프레임별 원재료는 ref/지역변수에만
 * 살고, setState는 값이 실제로 바뀌는 순간에만 일어난다.
 */
export function useGestureController({
  engine,
  stream,
  onAction,
  enabled = true,
}: UseGestureControllerOptions): UseGestureControllerResult {
  const [handVisible, setHandVisible] = useState(false);
  const [confirmProgress, setConfirmProgress] = useState(0);
  const [runtimeFailed, setRuntimeFailed] = useState(false);

  // 최신 콜백을 ref로 유지 — onAction이 바뀌어도 루프/리스너를 재구독하지 않는다
  const onActionRef = useRef(onAction);
  useEffect(() => {
    onActionRef.current = onAction;
  });

  // 키보드: 엔진 상태와 무관하게 enabled면 항상 동작 (운영자 개입 통로)
  useEffect(() => {
    if (!enabled) return;

    function handleKeyDown(event: KeyboardEvent) {
      const action = KEY_TO_ACTION[event.key];
      if (!action) return;
      // 키를 꾹 누를 때의 auto-repeat로 CONFIRM이 연발되면
      // 다음 스테이지까지 관통 확정될 수 있다 — 최초 keydown만 받는다.
      if (event.repeat) return;
      event.preventDefault();
      onActionRef.current({ action, source: "keyboard" });
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [enabled]);

  // 인식 루프: enabled + stream + recognizer가 모두 준비된 동안만 돈다
  useEffect(() => {
    const recognizer = engine.recognizer;
    if (!enabled || !stream || !recognizer) return;

    // WebcamBackground의 DOM을 건드리지 않도록 전용 숨은 video를 쓴다
    const video = document.createElement("video");
    video.muted = true;
    video.playsInline = true;
    video.srcObject = stream;
    // 빠른 마운트/해제(StrictMode·스테이지 전환) 때 cleanup이 srcObject를 끊으면
    // 진행 중이던 play()가 AbortError로 reject된다 — 무해한 레이스라 삼킨다.
    video.play().catch(() => {});

    const swipe = new SwipeDetector();
    const palm = new PalmHoldDetector();
    let rafId = 0;
    let consecutiveErrors = 0;
    let lastVideoTime = -1;
    // 프레임마다 무조건 setState하지 않도록 마지막 값을 추적 — 값이 실제로
    // 바뀔 때만 setHandVisible/setConfirmProgress를 호출한다
    let lastHandVisible: boolean | null = null;
    let lastProgress = -1;
    // 손 인식이 끊긴 첫 프레임의 시각. 보이는 동안은 null.
    let handLostAt: number | null = null;

    function tick() {
      rafId = requestAnimationFrame(tick);
      if (video.readyState < 2) return;
      if (video.currentTime === lastVideoTime) return; // 새 프레임 없음
      lastVideoTime = video.currentTime;

      const now = performance.now();
      try {
        // tick은 nested 함수라 TS가 바깥의 non-null 좁힘을 안으로 들고
        // 오지 못한다(TS18047) — recognizer는 const라 실제로는 항상
        // non-null이지만, 재확인 없이는 typecheck가 통과하지 않는다.
        if (!recognizer) {
          cancelAnimationFrame(rafId);
          return;
        }
        const result = recognizer.recognizeForVideo(video, now);
        consecutiveErrors = 0;

        const hand = result.landmarks[0];
        if (!hand || !hand[0]) {
          // 빠른 스와이프는 모션 블러로 손 인식이 200ms대로 잠깐 끊긴다.
          // 유예 시간 안의 끊김이면 스와이프 궤적을 유지해 공백을 잇는다.
          // 팜홀드는 "연속 유지"가 조건이므로 즉시 리셋이 맞다.
          handLostAt ??= now;
          if (now - handLostAt > HAND_LOST_GRACE_MS) {
            swipe.reset();
          }
          palm.reset();
          if (lastHandVisible !== false) {
            lastHandVisible = false;
            setHandVisible(false);
          }
          if (lastProgress !== 0) {
            lastProgress = 0;
            setConfirmProgress(0);
          }
          return;
        }
        handLostAt = null;
        if (lastHandVisible !== true) {
          lastHandVisible = true;
          setHandVisible(true);
        }

        const wristX = hand[0].x; // landmark 0 = 손목

        const swipeAction = swipe.update(wristX, now);
        if (swipeAction) {
          onActionRef.current({ action: swipeAction, source: "hand" });
        }

        const openPalmScore =
          result.gestures[0]?.find(
            (category) => category.categoryName === "Open_Palm",
          )?.score ?? 0;
        const { progress, confirmed } = palm.update({
          openPalmScore,
          x: wristX,
          timestampMs: now,
        });
        const roundedProgress = Math.round(progress * 100) / 100;
        if (lastProgress !== roundedProgress) {
          lastProgress = roundedProgress;
          setConfirmProgress(roundedProgress);
        }
        if (confirmed) {
          onActionRef.current({ action: "CONFIRM", source: "hand" });
        }
      } catch (error) {
        consecutiveErrors += 1;
        if (consecutiveErrors >= RUNTIME_ERROR_LIMIT) {
          console.error("Gesture recognition loop halted:", error);
          cancelAnimationFrame(rafId);
          setRuntimeFailed(true);
        }
      }
    }

    rafId = requestAnimationFrame(tick);

    return () => {
      cancelAnimationFrame(rafId);
      video.srcObject = null;
      setHandVisible(false);
      setConfirmProgress(0);
    };
  }, [enabled, stream, engine.recognizer]);

  return {
    status: runtimeFailed ? "ERROR" : engine.status,
    handVisible,
    confirmProgress,
  };
}
