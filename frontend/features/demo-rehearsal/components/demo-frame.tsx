"use client";

import { motion } from "framer-motion";
import type { ReactNode } from "react";
import type { DemoPhase } from "../types";

const PHASES: ReadonlyArray<{ id: DemoPhase; label: string }> = [
  { id: "briefing", label: "브리핑" },
  { id: "outfit", label: "옷 입히기" },
  { id: "simulation", label: "시뮬레이션" },
  { id: "ticket", label: "티켓 발급" },
];

export function DemoFrame({
  phase,
  children,
}: {
  phase: DemoPhase;
  children: ReactNode;
}) {
  const phaseIndex = PHASES.findIndex((item) => item.id === phase);
  const ticket = phase === "ticket";

  return (
    <div
      className={`relative h-full w-full overflow-hidden ${
        ticket ? "bg-[#e9eef1] text-[#172027]" : "text-white"
      }`}
    >
      {!ticket && (
        <>
          <div className="absolute inset-0 bg-gradient-to-b from-black/50 via-black/15 to-black/80" />
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,transparent_0%,rgba(0,0,0,0.18)_54%,rgba(0,0,0,0.7)_100%)]" />
        </>
      )}

      <div className="absolute top-[clamp(12px,2.5vh,24px)] right-[clamp(14px,3vw,32px)] left-[clamp(14px,3vw,32px)] z-30 flex items-center justify-between gap-3">
        <span
          className={`text-xs font-light tracking-[0.32em] whitespace-nowrap ${ticket ? "text-[#40515b]" : "text-white/70"}`}
        >
          DAILY REHEARSAL · DEMO
        </span>
        <div className="flex items-center gap-3">
          <span
            className={`text-xs font-light tracking-[0.16em] max-[480px]:hidden ${ticket ? "text-[#60707a]" : "text-white/60"}`}
          >
            {PHASES[phaseIndex]?.label}
          </span>
          <div className="flex gap-1.5">
            {PHASES.map((item, index) => (
              <div
                key={item.id}
                className={`h-1.5 w-[clamp(14px,2.6vw,28px)] rounded-full ${
                  index <= phaseIndex
                    ? ticket
                      ? "bg-[#00B0F0]"
                      : "bg-white/70"
                    : ticket
                      ? "bg-[#cbd5da]"
                      : "bg-white/15"
                }`}
              />
            ))}
          </div>
        </div>
      </div>

      <motion.div
        key={phase}
        className="absolute inset-0"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.6, ease: "easeInOut" }}
      >
        {children}
      </motion.div>

      <motion.div
        className={`absolute bottom-[clamp(10px,2vh,24px)] left-1/2 z-30 w-max max-w-[92%] -translate-x-1/2 rounded-full px-5 py-2 text-center font-light tracking-[0.18em] ${
          ticket
            ? "border border-[#cbd5da] bg-white/90 text-base text-[#4d5e68]"
            : "bg-black/45 text-sm text-white/80 backdrop-blur-sm"
        }`}
        animate={{ opacity: [0.55, 1, 0.55] }}
        transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
      >
        {phase === "outfit"
          ? "←/→ 또는 스와이프로 고르고 · Enter 또는 손바닥으로 선택"
          : phase === "ticket"
            ? "Enter를 눌러 데모 다시 시작"
            : "대본을 말한 뒤 Enter"}
      </motion.div>
    </div>
  );
}
