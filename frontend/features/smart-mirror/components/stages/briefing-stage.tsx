"use client";

import { motion } from "framer-motion";
import { Mic } from "lucide-react";
import { mockExperience } from "../../data/mock-experience";
import { SttPanel } from "../shared/stt-panel";

export function BriefingStage() {
  return (
    <div className="relative flex h-full flex-col items-center justify-center px-8">
      <EdgeWave />
      <motion.div
        className="absolute top-[17%] flex flex-col items-center gap-4 text-center"
        initial={{ opacity: 0, y: -14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7 }}
      >
        <div className="flex items-center gap-2 rounded-full border border-white/10 bg-black/20 px-4 py-2 text-white/70 backdrop-blur-xl">
          <Mic className="h-4 w-4" strokeWidth={1.5} />
          <span className="text-xs font-light tracking-[0.2em]">LISTENING</span>
        </div>
        <h1 className="text-4xl font-extralight tracking-wide md:text-6xl">
          내일 하루를 짧게 말해주세요
        </h1>
        <p className="max-w-2xl text-base font-light text-white/65 md:text-lg">
          일정, 걱정, 되고 싶은 모습까지 한 번에 말하면 거울이 내일의 장면을
          구성합니다.
        </p>
      </motion.div>

      <motion.div
        className="absolute inset-x-0 bottom-12 flex justify-center px-6"
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2, duration: 0.7 }}
      >
        <SttPanel text={mockExperience.transcript} />
      </motion.div>
    </div>
  );
}

function EdgeWave() {
  return (
    <div className="pointer-events-none absolute inset-0">
      <motion.div
        className="absolute inset-4 rounded-[2rem] border border-white/20"
        animate={{
          boxShadow: [
            "0 0 20px rgba(255,255,255,0.06)",
            "0 0 60px rgba(255,255,255,0.16)",
            "0 0 20px rgba(255,255,255,0.06)",
          ],
        }}
        transition={{ duration: 2.4, repeat: Infinity, ease: "easeInOut" }}
      />
      <motion.div
        className="absolute right-0 bottom-0 left-0 h-1 bg-gradient-to-r from-transparent via-white/55 to-transparent"
        animate={{ opacity: [0.25, 0.9, 0.25], scaleX: [0.65, 1, 0.65] }}
        transition={{ duration: 1.6, repeat: Infinity, ease: "easeInOut" }}
      />
    </div>
  );
}
