"use client";

import { useEffect, useState } from "react";
import { FilesetResolver, GestureRecognizer } from "@mediapipe/tasks-vision";
import {
  HAND_MIN_DETECTION_CONFIDENCE,
  HAND_MIN_PRESENCE_CONFIDENCE,
  HAND_MIN_TRACKING_CONFIDENCE,
} from "../lib/gesture/constants";
import type { GestureEngineHandle, GestureEngineStatus } from "../types";

const WASM_PATH = "/mediapipe/wasm";
const MODEL_PATH = "/mediapipe/gesture_recognizer.task";

// MediaPipe WASM은 내부 glog INFO/WARNING을 console.error/warn 채널로
// 출력한다(구글 측 알려진 동작). 실제 문제가 아닌데도 Next.js dev
// 오버레이가 "1 Issue"로 집계하므로, 개발 모드에서 해당 패턴만 걸러낸다.
const MEDIAPIPE_LOG_NOISE = [
  /^INFO: Created TensorFlow Lite/,
  /inference_feedback_manager\.cc/,
  /landmark_projection_calculator\.cc/,
];

let noiseFilterInstalled = false;

function installMediapipeLogNoiseFilter(): void {
  if (noiseFilterInstalled || process.env.NODE_ENV === "production") return;
  noiseFilterInstalled = true;

  for (const channel of ["error", "warn"] as const) {
    const original = console[channel].bind(console);
    console[channel] = (...args: unknown[]) => {
      const [first] = args;
      if (
        typeof first === "string" &&
        MEDIAPIPE_LOG_NOISE.some((pattern) => pattern.test(first))
      ) {
        return;
      }
      original(...args);
    };
  }
}

/**
 * MediaPipe GestureRecognizer를 로딩해 소유하는 훅. 세션 루트에서 1번 호출.
 * 카메라 권한과 무관하게 마운트 즉시 로딩을 시작한다(병렬 준비).
 * 스테이지는 반환된 핸들을 useGestureController에 넘겨 소비만 한다.
 */
export function useGestureEngine(): GestureEngineHandle {
  const [status, setStatus] = useState<GestureEngineStatus>("LOADING");
  const [recognizer, setRecognizer] = useState<GestureRecognizer | null>(null);

  useEffect(() => {
    let cancelled = false;
    let created: GestureRecognizer | null = null;

    async function load() {
      try {
        installMediapipeLogNoiseFilter();
        const fileset = await FilesetResolver.forVisionTasks(WASM_PATH);
        created = await createRecognizer(fileset);

        // 언마운트 후 늦게 resolve된 경우: 인스턴스 정리하고 종료
        if (cancelled) {
          created.close();
          return;
        }
        setRecognizer(created);
        setStatus("READY");
      } catch (error) {
        if (cancelled) return;
        console.error("Gesture engine failed to load:", error);
        setStatus("ERROR");
      }
    }

    void load();

    return () => {
      cancelled = true;
      created?.close();
    };
  }, []);

  return { status, recognizer };
}

type VisionFileset = Awaited<ReturnType<typeof FilesetResolver.forVisionTasks>>;

async function createRecognizer(
  fileset: VisionFileset,
): Promise<GestureRecognizer> {
  const options = {
    runningMode: "VIDEO" as const,
    numHands: 1,
    minHandDetectionConfidence: HAND_MIN_DETECTION_CONFIDENCE,
    minHandPresenceConfidence: HAND_MIN_PRESENCE_CONFIDENCE,
    minTrackingConfidence: HAND_MIN_TRACKING_CONFIDENCE,
  };
  try {
    return await GestureRecognizer.createFromOptions(fileset, {
      ...options,
      baseOptions: { modelAssetPath: MODEL_PATH, delegate: "GPU" },
    });
  } catch {
    // 일부 환경은 GPU delegate 초기화에 실패한다 — CPU로 재시도
    return await GestureRecognizer.createFromOptions(fileset, {
      ...options,
      baseOptions: { modelAssetPath: MODEL_PATH, delegate: "CPU" },
    });
  }
}
