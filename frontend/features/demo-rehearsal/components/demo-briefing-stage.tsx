"use client";

import { motion } from "framer-motion";
import { demoBriefing } from "../data/scenario";
import type { DemoBriefingStep } from "../types";
import {
  DemoCenter,
  DemoGlassPanel,
  DemoScanning,
  DemoStatus,
  DemoTranscript,
} from "./demo-ui";

export function DemoBriefingStage({ step }: { step: DemoBriefingStep }) {
  if (step === "INITIAL_TRANSCRIPT") {
    return (
      <DemoCenter>
        <DemoTranscript
          label="YOUR BRIEFING"
          text={demoBriefing.initial.transcript}
        />
        <DemoStatus text="답변을 확인했어요" />
      </DemoCenter>
    );
  }

  if (step === "FOLLOW_UP_TRANSCRIPT") {
    return (
      <DemoCenter>
        <DemoTranscript
          label="YOUR BRIEFING"
          text={demoBriefing.followUp.transcript}
        />
        <DemoStatus text="답변을 확인했어요" />
      </DemoCenter>
    );
  }

  if (step === "ANALYZING" || step === "MERGING") {
    return (
      <DemoCenter>
        <DemoScanning />
        <DemoStatus
          text={
            step === "ANALYZING"
              ? "내일의 상황을 정리하는 중…"
              : "답변을 반영하는 중…"
          }
        />
      </DemoCenter>
    );
  }

  const followUp = step === "FOLLOW_UP_QUESTION";
  const question = followUp
    ? demoBriefing.followUp.question
    : demoBriefing.initial.question;

  return (
    <div className="flex h-full flex-col items-center justify-center gap-8 px-[clamp(1rem,4vw,2rem)] pt-16 pb-20">
      <motion.div
        className="w-full max-w-4xl text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/65">
          {followUp ? "FOLLOW-UP" : "BRIEFING"}
        </p>
        <DemoGlassPanel className="px-[clamp(1.25rem,4vw,2.5rem)] py-[clamp(1.25rem,3vh,2rem)]">
          <h1 className="text-[clamp(1.35rem,3.8vw,2.75rem)] leading-snug font-extralight tracking-wide break-keep text-white">
            {question}
          </h1>
          {!followUp && (
            <div className="mt-6 border-t border-white/15 pt-5 text-left">
              <p className="text-xs font-medium tracking-[0.18em] text-white/50">
                이렇게 말해볼 수 있어요
              </p>
              <p className="mt-2 text-[clamp(1rem,2vw,1.25rem)] leading-relaxed font-light break-keep text-white/80">
                “{demoBriefing.initial.example}”
              </p>
            </div>
          )}
        </DemoGlassPanel>
      </motion.div>

      <div className="flex flex-col items-center gap-2">
        <motion.span
          className="text-5xl"
          animate={{ opacity: [0.45, 1, 0.45] }}
          transition={{ duration: 1.6, repeat: Infinity }}
          aria-hidden
        >
          🎙️
        </motion.span>
        <DemoStatus text="편하게 이야기해 주세요 — 듣고 있어요" />
      </div>
    </div>
  );
}
