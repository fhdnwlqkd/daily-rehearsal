"use client";

import { useState } from "react";
import { AnimatePresence } from "framer-motion";
import { WebcamBackground } from "./components/webcam-background";
import { PermissionGuide } from "./components/permission-guide";
import { ExperienceSession } from "./components/experience-session";
import { useCamera } from "./hooks/use-camera";
import { useGestureEngine } from "./hooks/use-gesture-engine";

/**
 * 부스 수명 층. 전시 내내 유지되는 장비(카메라 stream, 추후 제스처 engine)만
 * 소유하고, 관람객 한 명의 세션 상태는 전부 ExperienceSession이 가진다.
 * 세션 초기화 = sessionEpoch 증가 → key 리마운트 (수동 state 리셋 금지 —
 * frontend/CLAUDE.md "수명 2층 구조").
 */
export function SmartMirror() {
  const [sessionEpoch, setSessionEpoch] = useState(0);
  // 카메라/마이크 단일 소유권. stream은 WebcamBackground로 내려준다.
  const { stream, status, retry } = useCamera();
  // 제스처 엔진(MediaPipe)도 부스 수명 — 모델 로딩이 수 초라 세션마다 재로딩 금지.
  const engine = useGestureEngine();

  const showPermissionGuide = status === "denied";

  return (
    // h-dvh: iframe·모바일에서 주소창 변동에도 실제 보이는 높이를 채운다(#232).
    // cursor-none은 부스 키오스크 잔재 — 온라인 전시(데스크톱 마우스)에서는 커서가 보여야 한다.
    <div className="relative h-dvh w-full overflow-hidden bg-black">
      {/* Permission Guide Overlay */}
      <AnimatePresence>
        {showPermissionGuide && <PermissionGuide onRetry={retry} />}
      </AnimatePresence>

      {/* Webcam Background Layer (z-0) — 세션이 리셋돼도 깜빡이지 않는다 */}
      <WebcamBackground stream={stream} />

      {/* Session Layer (z-10) */}
      <ExperienceSession
        key={sessionEpoch}
        engine={engine}
        stream={stream}
        onRestart={() => setSessionEpoch((epoch) => epoch + 1)}
      />
    </div>
  );
}
