"use client";

import { motion } from "framer-motion";
import type { ComponentType } from "react";
import type { ExperiencePhase, ExperiencePhaseId } from "../types";
import { BriefingStage } from "./stages/briefing-stage";
import { ContextStage } from "./stages/context-stage";
import { TransformationStage } from "./stages/transformation-stage";
import { GestureFitStage } from "./stages/gesture-fit-stage";
import { RehearsalStage } from "./stages/rehearsal-stage";
import { ChangeCardStage } from "./stages/change-card-stage";

// phase.id → 스테이지 컴포넌트 매핑. Record라서 단계를 추가하면
// 여기 키 누락이 컴파일 에러로 강제된다(exhaustiveness 보장).
const STAGE_COMPONENTS: Record<ExperiencePhaseId, ComponentType> = {
  briefing: BriefingStage,
  context: ContextStage,
  transformation: TransformationStage,
  "gesture-fit": GestureFitStage,
  rehearsal: RehearsalStage,
  "change-card": ChangeCardStage,
};

interface ExperienceStageProps {
  phase: ExperiencePhase;
  phaseIndex: number;
  totalPhases: number;
}

export function ExperienceStage({
  phase,
  phaseIndex,
  totalPhases,
}: ExperienceStageProps) {
  const Stage = STAGE_COMPONENTS[phase.id];

  return (
    <div className="relative h-full w-full overflow-hidden text-white">
      <MirrorToneOverlay phase={phase.id} />
      <StageHeader
        phase={phase}
        phaseIndex={phaseIndex}
        totalPhases={totalPhases}
      />
      {phase.id !== "briefing" && <TransitionCue phase={phase.id} />}

      <Stage />

      <TapHint isLast={phaseIndex === totalPhases - 1} />
    </div>
  );
}

function MirrorToneOverlay({ phase }: { phase: ExperiencePhaseId }) {
  const overlays: Record<ExperiencePhaseId, string> = {
    briefing: "from-black/35 via-transparent to-black/55",
    context: "from-black/55 via-black/10 to-black/75",
    transformation: "from-black/45 via-black/10 to-black/75",
    "gesture-fit": "from-black/45 via-black/10 to-black/75",
    rehearsal: "from-black/45 via-black/20 to-black/80",
    "change-card": "from-black/70 via-black/55 to-black/90",
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
      <div className="flex items-center gap-3 text-white/70">
        <span className="text-xs font-light tracking-[0.32em]">
          DAILY REHEARSAL
        </span>
        <span className="h-px w-10 bg-white/20" />
        <span className="text-xs font-light tracking-[0.18em]">
          {phase.timeRange}
        </span>
      </div>
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
    briefing: "",
    context: "맥락을 정리합니다",
    transformation: "내일의 모습을 입혀봅니다",
    "gesture-fit": "제스처로 모습을 고릅니다",
    rehearsal: "결정적 순간을 들어봅니다",
    "change-card": "내일의 변화 카드를 발급합니다",
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

function TapHint({ isLast }: { isLast: boolean }) {
  return (
    <motion.div
      className="absolute bottom-6 left-1/2 z-30 -translate-x-1/2 text-center text-xs font-light tracking-[0.2em] text-white/45"
      animate={{ opacity: [0.25, 0.75, 0.25] }}
      transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
    >
      {isLast
        ? "탭하거나 Enter를 눌러 다시 시작"
        : "탭하거나 Enter를 눌러 계속"}
    </motion.div>
  );
}
