"use client";

import { useEffect, useRef, useState } from "react";
import {
  GESTURE_CROP_MARGIN,
  GESTURE_INPUT_MAX_SIDE,
  HAND_LOST_GRACE_MS,
  RUNTIME_ERROR_LIMIT,
} from "../lib/gesture/constants";
import {
  computeVisibleSourceRect,
  type SourceRect,
} from "../lib/gesture/visible-rect";
import {
  clearGestureDebugFrame,
  publishGestureDebugFrame,
} from "../lib/gesture/debug-bus";
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

    // 인식 입력 전용 캔버스 — 카메라 프레임 전체가 아니라 "화면에 보이는
    // 영역"만 여기로 옮겨 담아 인식에 넘긴다. 전시장 카메라가 세로 모니터보다
    // 훨씬 넓게 찍어서, 자르지 않으면 화면 밖 관람객의 손이 후보로 들어온다.
    const input = document.createElement("canvas");
    const inputContext = input.getContext("2d", { willReadFrequently: false });

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
    // 디버그 오버레이용 누적치 — 오버레이가 꺼져 있어도 계산은 가볍다(사칙연산).
    let lastFrameAt = 0;
    let fps = 0;
    let lastAction: GestureAction | null = null;
    let lastActionAtMs = 0;
    // 이번 프레임에 실제로 인식에 넘긴 영역(카메라 픽셀 좌표) — 랜드마크는
    // 이 사각형 기준으로 정규화돼 돌아오므로 디버그 오버레이가 되돌릴 때 쓴다.
    let crop: SourceRect | null = null;

    /**
     * 보이는 영역만 캔버스로 옮겨 담고 그 캔버스를 인식 입력으로 돌려준다.
     * 크롭이 불가능한 상태(메타데이터 전·2D 컨텍스트 없음)면 원본 video를
     * 그대로 쓴다 — 인식이 멈추는 것보다 넓게라도 도는 편이 낫다.
     */
    function prepareInput(): HTMLVideoElement | HTMLCanvasElement {
      if (!inputContext || video.videoWidth <= 0) {
        crop = null;
        return video;
      }

      const rect = computeVisibleSourceRect({
        videoWidth: video.videoWidth,
        videoHeight: video.videoHeight,
        // 뷰포트를 매 프레임 읽는다 — 회전·리사이즈에 리스너 없이 따라간다.
        displayWidth: window.innerWidth,
        displayHeight: window.innerHeight,
        margin: GESTURE_CROP_MARGIN,
      });

      // 크롭이 바뀌면 정규화 기준(프레임 폭)이 바뀐다 — 이전 궤적과 이어
      // 붙이면 손이 가만히 있어도 dx가 튀어 헛스와이프가 발사된다.
      // 전체화면 전환·회전처럼 드문 사건이라 궤적을 버리는 쪽이 안전하다.
      if (crop && (crop.sw !== rect.sw || crop.sx !== rect.sx)) {
        swipe.reset();
        palm.reset();
      }

      const shrink = Math.min(
        1,
        GESTURE_INPUT_MAX_SIDE / Math.max(rect.sw, rect.sh),
      );
      const width = Math.max(1, Math.round(rect.sw * shrink));
      const height = Math.max(1, Math.round(rect.sh * shrink));
      if (input.width !== width || input.height !== height) {
        input.width = width;
        input.height = height;
      }

      inputContext.drawImage(
        video,
        rect.sx,
        rect.sy,
        rect.sw,
        rect.sh,
        0,
        0,
        width,
        height,
      );
      crop = rect;
      return input;
    }

    /** 발사된 액션을 상위로 올리면서 디버그 기록도 같이 남긴다 */
    function emit(action: GestureAction, timestampMs: number) {
      lastAction = action;
      lastActionAtMs = timestampMs;
      onActionRef.current({ action, source: "hand" });
    }

    function tick() {
      rafId = requestAnimationFrame(tick);
      if (video.readyState < 2) return;
      if (video.currentTime === lastVideoTime) return; // 새 프레임 없음
      lastVideoTime = video.currentTime;

      const now = performance.now();
      // 지수 평활 — 프레임마다 튀는 순간 FPS 대신 읽을 수 있는 값을 만든다
      if (lastFrameAt > 0) {
        const instantFps = 1000 / Math.max(1, now - lastFrameAt);
        fps = fps === 0 ? instantFps : fps * 0.8 + instantFps * 0.2;
      }
      lastFrameAt = now;
      try {
        // tick은 nested 함수라 TS가 바깥의 non-null 좁힘을 안으로 들고
        // 오지 못한다(TS18047) — recognizer는 const라 실제로는 항상
        // non-null이지만, 재확인 없이는 typecheck가 통과하지 않는다.
        if (!recognizer) {
          cancelAnimationFrame(rafId);
          return;
        }
        const result = recognizer.recognizeForVideo(prepareInput(), now);
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
          publishGestureDebugFrame({
            timestampMs: now,
            landmarks: null,
            videoWidth: video.videoWidth,
            videoHeight: video.videoHeight,
            crop,
            inputCanvas: crop ? input : null,
            gestureName: null,
            gestureScore: 0,
            openPalmScore: 0,
            palmGate: "NO_HAND",
            palmSpeed: 0,
            swipeDx: swipe.displacement(),
            confirmProgress: 0,
            fps,
            lastAction,
            lastActionAtMs,
          });
          return;
        }
        handLostAt = null;
        if (lastHandVisible !== true) {
          lastHandVisible = true;
          setHandVisible(true);
        }

        const wristX = hand[0].x; // landmark 0 = 손목
        // 스와이프는 손끝(landmark 12, 중지 끝)을 본다: 손목을 축으로
        // "까닥"하는 자연 스와이프는 손목 이동량이 거의 0이라 손목
        // 기준으론 신호가 안 생긴다. 평행이동 스와이프에선 손끝도
        // 손목만큼 움직이므로 기존 동작은 그대로다. 팜홀드의 정지
        // 판정은 회전에 흔들리지 않는 손목을 유지한다.
        const fingertipX = hand[12]?.x ?? wristX;

        const swipeAction = swipe.update(fingertipX, now);
        if (swipeAction) {
          emit(swipeAction, now);
        }

        const categories = result.gestures[0] ?? [];
        const openPalmScore =
          categories.find((category) => category.categoryName === "Open_Palm")
            ?.score ?? 0;
        const topCategory = categories[0] ?? null;
        const { progress, confirmed, gate, speed } = palm.update({
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
          emit("CONFIRM", now);
        }

        publishGestureDebugFrame({
          timestampMs: now,
          landmarks: hand,
          videoWidth: video.videoWidth,
          videoHeight: video.videoHeight,
          crop,
          inputCanvas: crop ? input : null,
          gestureName: topCategory?.categoryName ?? null,
          gestureScore: topCategory?.score ?? 0,
          openPalmScore,
          palmGate: gate,
          palmSpeed: speed,
          swipeDx: swipe.displacement(),
          confirmProgress: roundedProgress,
          fps,
          lastAction,
          lastActionAtMs,
        });
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
      clearGestureDebugFrame();
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
