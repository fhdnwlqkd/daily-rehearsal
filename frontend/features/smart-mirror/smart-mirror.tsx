"use client";

import { useState, useEffect, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { WebcamBackground } from "./components/webcam-background";
import { PermissionGuide } from "./components/permission-guide";
import { ExperienceStage } from "./components/stage-router";
import { useCamera } from "./hooks/use-camera";
import { experiencePhases } from "./data/phases";

export function SmartMirror() {
  const [phaseIndex, setPhaseIndex] = useState(0);
  const [showDebug, setShowDebug] = useState(false);
  // 카메라/마이크 단일 소유권. stream은 WebcamBackground로 내려준다.
  const { stream, status, retry } = useCamera();

  const showPermissionGuide = status === "denied";
  const currentPhase = experiencePhases[phaseIndex];

  const goToNextPhase = useCallback(() => {
    setPhaseIndex((current) => (current + 1) % experiencePhases.length);
  }, []);

  const goToPhase = useCallback((index: number) => {
    setPhaseIndex(index);
  }, []);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === " " || event.key === "Enter") {
        event.preventDefault();
        goToNextPhase();
      }

      if (event.key.toLowerCase() === "d") {
        setShowDebug((visible) => !visible);
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [goToNextPhase]);

  if (!currentPhase) return null;

  return (
    <div
      className="relative h-screen w-screen cursor-none overflow-hidden bg-black"
      onClick={goToNextPhase}
      role="presentation"
    >
      {/* Permission Guide Overlay */}
      <AnimatePresence>
        {showPermissionGuide && <PermissionGuide onRetry={retry} />}
      </AnimatePresence>

      {/* Webcam Background Layer (z-0) */}
      <WebcamBackground stream={stream} />

      {/* State Content Layer */}
      <AnimatePresence mode="wait">
        <motion.div
          key={currentPhase.id}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.65, ease: "easeInOut" }}
          className="absolute inset-0 z-10"
        >
          <ExperienceStage
            phase={currentPhase}
            phaseIndex={phaseIndex}
            totalPhases={experiencePhases.length}
          />
        </motion.div>
      </AnimatePresence>

      {showDebug && (
        <div className="absolute top-6 right-6 z-50 flex gap-2">
          {experiencePhases.map((phase, index) => (
            <motion.button
              key={phase.id}
              onClick={(event) => {
                event.stopPropagation();
                goToPhase(index);
              }}
              className={`flex h-10 min-w-10 items-center justify-center rounded-xl border px-3 text-xs backdrop-blur-xl transition-all ${
                phaseIndex === index
                  ? "border-white/40 bg-white/20 text-white"
                  : "border-white/10 bg-white/5 text-white/60 hover:border-white/20 hover:bg-white/10 hover:text-white/80"
              }`}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              {index + 1}
            </motion.button>
          ))}
        </div>
      )}
    </div>
  );
}
