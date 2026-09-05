"use client";

import { Camera, RotateCcw } from "lucide-react";
import { useGestureEngine } from "@/features/smart-mirror";
import { DemoSession } from "./components/demo-session";
import { useDemoCamera } from "./hooks/use-demo-camera";

export function DemoRehearsal() {
  const camera = useDemoCamera();
  const engine = useGestureEngine();

  return (
    <main className="relative h-dvh w-full overflow-hidden bg-black">
      <DemoSession engine={engine} cameraStream={camera.stream} />

      {camera.status === "PENDING" && (
        <div className="absolute inset-0 z-[100] flex items-center justify-center bg-black text-white">
          <p className="text-lg font-extralight tracking-wide">
            발표용 카메라를 준비하는 중…
          </p>
        </div>
      )}

      {camera.status === "ERROR" && (
        <div className="absolute inset-0 z-[100] flex items-center justify-center bg-black/90 px-8 text-white backdrop-blur-md">
          <div className="max-w-lg text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-white/10">
              <Camera className="h-8 w-8" />
            </div>
            <h1 className="mt-6 text-3xl font-light">
              카메라 권한이 필요합니다
            </h1>
            <p className="mt-4 leading-relaxed font-light text-white/60">
              주소창 왼쪽의 사이트 설정에서 카메라를 허용한 뒤 다시
              시도해주세요. 데모에서는 마이크와 화면 녹화를 사용하지 않습니다.
            </p>
            <button
              type="button"
              onClick={camera.retry}
              className="mt-7 inline-flex cursor-pointer items-center gap-2 rounded-full bg-white px-6 py-3 font-medium text-black"
            >
              <RotateCcw className="h-4 w-4" />
              다시 시도
            </button>
          </div>
        </div>
      )}
    </main>
  );
}
