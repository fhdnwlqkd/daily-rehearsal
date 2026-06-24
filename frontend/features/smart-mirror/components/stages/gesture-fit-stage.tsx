"use client";

import { motion } from "framer-motion";
import { ArrowLeft, ArrowRight, Sparkles } from "lucide-react";
import { mockExperience } from "../../data/mock-experience";
import { GlassPanel } from "../shared/glass-panel";

export function GestureFitStage() {
  return (
    <div className="relative flex h-full items-center justify-center px-8 pt-20">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(255,255,255,0.08),transparent_44%)]" />
      <motion.div
        className="absolute inset-x-0 top-[15%] z-20 flex flex-col items-center gap-4 text-center"
        initial={{ opacity: 0, y: -14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.65 }}
      >
        <div className="flex items-center gap-2 rounded-full border border-white/15 bg-white/8 px-4 py-2 text-white/75 backdrop-blur-xl">
          <Sparkles className="h-4 w-4" strokeWidth={1.5} />
          <span className="text-xs font-light tracking-[0.22em]">
            GESTURE FITTING
          </span>
        </div>
        <h1 className="text-4xl font-extralight tracking-wide md:text-6xl">
          손짓으로 내일의 모습을 골라보세요
        </h1>
        <p className="max-w-2xl text-base font-light text-white/65 md:text-lg">
          거울이 사용자를 인식하는 동안 Decart preview를 준비하고, 제스처로
          입어볼 옷을 바꿉니다.
        </p>
      </motion.div>
      <motion.div
        className="relative mt-28 h-[440px] w-[360px]"
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.6 }}
      >
        <div className="absolute inset-x-12 top-10 h-24 rounded-full border border-white/35" />
        <div className="absolute inset-x-4 top-36 h-52 rounded-[42%] border border-white/25" />
        <div className="absolute top-0 left-1/2 h-full w-px -translate-x-1/2 bg-gradient-to-b from-transparent via-white/25 to-transparent" />
        <motion.div
          className="absolute top-1/2 left-1/2 h-28 w-28 -translate-x-1/2 -translate-y-1/2 rounded-full border border-white/25"
          animate={{ scale: [0.82, 1.18, 0.82], opacity: [0.25, 0.75, 0.25] }}
          transition={{ duration: 2.1, repeat: Infinity, ease: "easeInOut" }}
        />
        <motion.div
          className="absolute top-1/2 -left-10 flex -translate-y-1/2 items-center gap-2 text-white/55"
          animate={{ x: [-8, 0, -8], opacity: [0.45, 0.85, 0.45] }}
          transition={{ duration: 1.8, repeat: Infinity, ease: "easeInOut" }}
        >
          <ArrowLeft className="h-5 w-5" strokeWidth={1.4} />
          <span className="text-xs font-light tracking-[0.18em]">PREV</span>
        </motion.div>
        <motion.div
          className="absolute top-1/2 -right-10 flex -translate-y-1/2 items-center gap-2 text-white/55"
          animate={{ x: [8, 0, 8], opacity: [0.45, 0.85, 0.45] }}
          transition={{ duration: 1.8, repeat: Infinity, ease: "easeInOut" }}
        >
          <span className="text-xs font-light tracking-[0.18em]">NEXT</span>
          <ArrowRight className="h-5 w-5" strokeWidth={1.4} />
        </motion.div>
        <motion.div
          className="absolute inset-0 rounded-[2rem] border border-white/18"
          animate={{
            boxShadow: [
              "0 0 0 rgba(255,255,255,0)",
              "0 0 60px rgba(255,255,255,0.16)",
              "0 0 0 rgba(255,255,255,0)",
            ],
          }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
        />
        <div className="absolute inset-x-0 bottom-14 px-8">
          <ProgressCharge />
        </div>
      </motion.div>
      <div className="absolute inset-x-0 bottom-12 flex justify-center px-6">
        <GlassPanel
          pulsing
          pulseColor="rgba(255, 255, 255, 0.32)"
          className="max-w-3xl text-center"
        >
          <p className="mb-2 text-xs font-light tracking-[0.26em] text-white/55">
            DECART PREVIEW 준비 중
          </p>
          <p className="text-3xl font-extralight tracking-wide">
            손짓으로 옷을 넘겨보세요
          </p>
          <p className="mt-3 text-sm font-light text-white/55">
            {mockExperience.gestureHint}
          </p>
        </GlassPanel>
      </div>
    </div>
  );
}

function ProgressCharge() {
  return (
    <div className="rounded-full border border-white/10 bg-black/40 p-2 backdrop-blur-xl">
      <motion.div
        className="h-3 rounded-full bg-gradient-to-r from-white/45 via-white/75 to-white"
        initial={{ width: "8%" }}
        animate={{ width: ["8%", "42%", "72%", "100%"] }}
        transition={{
          duration: 3,
          repeat: Infinity,
          repeatDelay: 0.6,
          ease: "easeInOut",
        }}
      />
    </div>
  );
}
