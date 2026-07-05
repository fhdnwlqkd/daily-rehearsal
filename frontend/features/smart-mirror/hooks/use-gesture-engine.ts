"use client";

import { useEffect, useState } from "react";
import { FilesetResolver, GestureRecognizer } from "@mediapipe/tasks-vision";
import type { GestureEngineHandle, GestureEngineStatus } from "../types";

const WASM_PATH = "/mediapipe/wasm";
const MODEL_PATH = "/mediapipe/gesture_recognizer.task";

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
  const options = { runningMode: "VIDEO" as const, numHands: 1 };
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
