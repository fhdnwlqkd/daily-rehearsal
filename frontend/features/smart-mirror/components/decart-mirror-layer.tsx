"use client";

import { useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";

interface DecartMirrorLayerProps {
  /** Decart 변환 출력. null이면 아무것도 그리지 않는다(원본 거울이 비친다). */
  stream: MediaStream | null;
}

/**
 * Decart 변환 스트림을 원본 거울(WebcamBackground) 위에 겹쳐 그리는 레이어.
 * 세션 층이 스테이지 아래에 깔아둔다 — 스테이지 전환(언마운트)에도 프리뷰가
 * 유지되고, 연결이 죽으면 페이드아웃되며 원본 거울로 자연 복귀한다.
 * 원본과 같은 scaleX(-1) 미러링으로 "같은 거울"처럼 보이게 한다.
 */
export function DecartMirrorLayer({ stream }: DecartMirrorLayerProps) {
  return (
    <AnimatePresence>
      {stream && (
        <motion.div
          className="absolute inset-0"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.8, ease: "easeInOut" }}
        >
          <MirrorVideo stream={stream} />
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function MirrorVideo({ stream }: { stream: MediaStream }) {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.srcObject = stream;
    }
  }, [stream]);

  return (
    <video
      ref={videoRef}
      autoPlay
      playsInline
      muted
      className="h-full w-full object-cover"
      style={{ transform: "scaleX(-1)" }}
    />
  );
}
