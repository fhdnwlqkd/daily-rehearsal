"use client";

import { motion } from "framer-motion";
import { demoSimulationTurns } from "../data/scenario";
import type { DemoSimulationStep } from "../types";
import {
  DemoCenter,
  DemoGlassPanel,
  DemoScanning,
  DemoStatus,
  DemoTranscript,
} from "./demo-ui";

export function DemoSimulationStage({
  turnIndex,
  step,
}: {
  turnIndex: number;
  step: DemoSimulationStep;
}) {
  const turn = demoSimulationTurns[turnIndex] ?? demoSimulationTurns[0];
  const turnNo = turnIndex + 1;

  if (step === "INTRO") {
    return (
      <DemoCenter>
        <motion.div
          className="max-w-4xl text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <p className="mb-5 text-sm font-normal tracking-[0.34em] text-white/90">
            SIMULATION · TURN {turnNo} / {demoSimulationTurns.length}
          </p>
          <p className="text-[clamp(1.4rem,4vw,3rem)] leading-snug font-extralight break-keep text-white">
            {turn.sceneCue}
          </p>
        </motion.div>
      </DemoCenter>
    );
  }

  if (step === "TRANSCRIPT") {
    return (
      <DemoCenter>
        <DemoTranscript label="YOUR ANSWER" text={turn.transcript} />
        <DemoStatus text="답변을 확인했어요" />
      </DemoCenter>
    );
  }

  if (step === "EVALUATING") {
    return (
      <DemoCenter>
        <DemoTranscript label="YOUR ANSWER" text={turn.transcript} />
        <DemoScanning />
        <DemoStatus text="상대가 당신의 말을 듣고 있어요…" />
      </DemoCenter>
    );
  }

  if (step === "FEEDBACK") {
    const accepted = turn.outcome === "ACCEPTED";
    return (
      <DemoCenter>
        {accepted && (
          <motion.div
            className="mb-2 text-center"
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
          >
            <p className="mb-3 text-xs font-light tracking-[0.34em] text-white/65">
              REHEARSAL COMPLETE
            </p>
            <h1 className="text-[clamp(1.7rem,4.5vw,3.2rem)] font-extralight tracking-wide">
              리허설 완료
            </h1>
          </motion.div>
        )}
        <DemoGlassPanel
          className={`w-full max-w-3xl px-8 py-6 text-center ${
            accepted ? "border-white/50" : "border-amber-200/35"
          }`}
        >
          <p
            className={`text-xs font-medium tracking-[0.32em] ${accepted ? "text-white/60" : "text-amber-100/75"}`}
          >
            {accepted ? "GOOD" : "COACHING"}
          </p>
          <p className="mt-3 text-[clamp(1.1rem,2.4vw,1.5rem)] leading-relaxed font-extralight break-keep text-white/95">
            {turn.feedback}
          </p>
        </DemoGlassPanel>
        <DemoStatus
          text={
            accepted
              ? "Enter를 눌러 결과 티켓을 확인하세요"
              : "Enter를 눌러 개선된 첫 문장을 연습하세요"
          }
        />
      </DemoCenter>
    );
  }

  return (
    <div className="flex h-full flex-col items-center justify-center gap-7 px-[clamp(1rem,4vw,2rem)] pt-16 pb-20">
      <div className="max-w-4xl text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]">
        <p className="mb-4 text-sm font-normal tracking-[0.3em] text-white/75">
          TURN {turnNo} / {demoSimulationTurns.length}
        </p>
        <DemoGlassPanel className="px-[clamp(1.25rem,4vw,2.5rem)] py-[clamp(1.25rem,3vh,2rem)]">
          <p className="text-[clamp(1.3rem,3.5vw,2.5rem)] leading-snug font-extralight break-keep">
            “{turn.opponentLine}”
          </p>
          <div className="mt-5 border-t border-white/15 pt-4">
            <p className="text-sm font-light tracking-wide text-white/70">
              {turn.actionPrompt}
            </p>
          </div>
        </DemoGlassPanel>
      </div>
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
