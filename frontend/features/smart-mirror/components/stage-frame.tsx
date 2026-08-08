"use client";

import { motion } from "framer-motion";
import type { ReactNode } from "react";
import type { ExperiencePhase, ExperiencePhaseId } from "../types";

interface StageFrameProps {
  phase: ExperiencePhase;
  phaseIndex: number;
  totalPhases: number;
  /** 현재 스테이지 화면. 어떤 스테이지를 그릴지·props 주입은 세션 층(ExperienceSession) 책임. */
  children: ReactNode;
}

/**
 * 모든 스테이지가 공유하는 공통 프레임 — 톤 오버레이·헤더(진행 표시)·
 * 전환 연출·조작 힌트를 씌우고 받은 스테이지(children)를 그린다.
 * REC 인디케이터처럼 여러 스테이지에 걸치는 표시도 이 레벨에 둔다.
 */
export function StageFrame({
  phase,
  phaseIndex,
  totalPhases,
  children,
}: StageFrameProps) {
  return (
    <div className="relative h-full w-full overflow-hidden text-white">
      <MirrorToneOverlay phase={phase.id} />
      <StageHeader
        phase={phase}
        phaseIndex={phaseIndex}
        totalPhases={totalPhases}
      />
      {phase.id !== "type-select" && <TransitionCue phase={phase.id} />}

      {children}

      <TapHint phase={phase.id} />
    </div>
  );
}

function MirrorToneOverlay({ phase }: { phase: ExperiencePhaseId }) {
  // 타입 선택은 화면 중앙에 제목·카드·힌트가 몰리는데 via가 투명하면
  // 생영상 위에 글자가 얹혀 전시 조명에 씻긴다 — 중앙에도 스크림을 깐다.
  const overlays: Record<ExperiencePhaseId, string> = {
    "type-select": "from-black/40 via-black/20 to-black/65",
    briefing: "from-black/55 via-black/10 to-black/75",
    outfit: "from-black/45 via-black/10 to-black/75",
    simulation: "from-black/45 via-black/20 to-black/80",
    ticket: "from-black/70 via-black/55 to-black/90",
  };

  return (
    <>
      <div className={`absolute inset-0 bg-gradient-to-b ${overlays[phase]}`} />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,transparent_0%,rgba(0,0,0,0.18)_54%,rgba(0,0,0,0.7)_100%)]" />
    </>
  );
}

function StageHeader({
  phase,
  phaseIndex,
  totalPhases,
}: {
  phase: ExperiencePhase;
  phaseIndex: number;
  totalPhases: number;
}) {
  return (
    <div className="absolute top-6 right-8 left-8 z-20 flex items-center justify-between">
      <span className="text-xs font-light tracking-[0.32em] text-white/70">
        DAILY REHEARSAL
      </span>
      <div className="flex items-center gap-3">
        <span className="text-xs font-light tracking-[0.16em] text-white/60">
          {phase.label}
        </span>
        <div className="flex gap-1.5">
          {Array.from({ length: totalPhases }).map((_, index) => (
            <div
              key={index}
              className={`h-1.5 w-7 rounded-full ${index <= phaseIndex ? "bg-white/70" : "bg-white/15"}`}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

function TransitionCue({ phase }: { phase: ExperiencePhaseId }) {
  const copy: Record<ExperiencePhaseId, string> = {
    "type-select": "",
    briefing: "내일의 상황을 그려봅니다",
    outfit: "내일의 모습을 입혀봅니다",
    simulation: "결정적 순간을 연습합니다",
    ticket: "내일의 티켓을 발급합니다",
  };

  return (
    <motion.div
      className="pointer-events-none absolute inset-0 z-40 flex items-center justify-center bg-black/35 backdrop-blur-[2px]"
      initial={{ opacity: 0 }}
      animate={{ opacity: [0, 1, 1, 0] }}
      transition={{
        duration: 2.4,
        times: [0, 0.16, 0.72, 1],
        ease: "easeInOut",
      }}
    >
      <motion.div
        className="border-y border-white/20 px-10 py-8 text-center"
        initial={{ y: 18, scale: 0.98 }}
        animate={{ y: [18, 0, 0, -10], scale: [0.98, 1, 1, 0.99] }}
        transition={{
          duration: 2.4,
          times: [0, 0.16, 0.72, 1],
          ease: "easeInOut",
        }}
      >
        <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/55">
          NEXT SEQUENCE
        </p>
        <p className="text-5xl font-extralight tracking-wide text-white md:text-7xl">
          {copy[phase]}
        </p>
      </motion.div>
    </motion.div>
  );
}

function TapHint({ phase }: { phase: ExperiencePhaseId }) {
  // 타입 선택의 제스처 안내는 스테이지 안(GestureHint)에서 상황별로 보여주므로
  // 여기서는 키보드 대체 입력만 알린다. 나머지는 개발용 진행 힌트.
  const copy: Record<ExperiencePhaseId, string> = {
    "type-select": "키보드 ←/→·Enter로도 조작할 수 있어요",
    briefing: "마이크에 대고 답해주세요 · Enter 바로 전송 · ← 다시 말하기",
    outfit: "키보드 ←/→·Enter로도 조작할 수 있어요",
    simulation: "탭하거나 Enter를 눌러 계속",
    ticket: "탭하거나 Enter를 눌러 다시 시작",
  };

  return (
    <motion.div
      className="absolute bottom-6 left-1/2 z-30 -translate-x-1/2 rounded-full bg-black/40 px-5 py-2 text-center text-sm font-light tracking-[0.2em] text-white/80 backdrop-blur-sm"
      animate={{ opacity: [0.45, 0.9, 0.45] }}
      transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
    >
      {copy[phase]}
    </motion.div>
  );
}
