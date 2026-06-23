"use client";

import { motion } from "framer-motion";
import { Mic, Volume2 } from "lucide-react";
import { mockExperience } from "../../data/mock-experience";
import { AudioWave } from "../shared/audio-wave";

export function RehearsalStage() {
  return (
    <div className="relative h-full px-8">
      <div className="absolute inset-0 bg-black/25 backdrop-blur-[1px]" />
      <div className="relative z-10 flex h-full items-center justify-center">
        <div className="grid w-full max-w-6xl grid-cols-[0.9fr_1.1fr] items-center gap-10">
          <motion.div
            className="rounded-[2rem] border border-white/10 bg-black/30 p-8 backdrop-blur-2xl"
            initial={{ opacity: 0, x: -24 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6 }}
          >
            <div className="mb-6 flex items-center gap-4">
              <div className="flex h-16 w-16 items-center justify-center rounded-full border border-white/15 bg-white/10">
                <Volume2 className="h-7 w-7 text-white/70" strokeWidth={1.3} />
              </div>
              <div>
                <p className="text-xs font-light tracking-[0.22em] text-white/45">
                  AI COUNTERPART
                </p>
                <p className="mt-1 text-lg font-light text-white/80">
                  상대의 첫 한마디
                </p>
              </div>
            </div>
            <p className="text-4xl leading-snug font-extralight tracking-wide">
              “{mockExperience.aiPrompt}”
            </p>
            <div className="mt-8">
              <AudioWave />
            </div>
          </motion.div>

          <motion.div
            className="rounded-[2rem] border border-white/10 bg-black/35 p-8 backdrop-blur-2xl"
            initial={{ opacity: 0, x: 24 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6, delay: 0.15 }}
          >
            <div className="mb-6 flex items-center justify-between">
              <div className="flex items-center gap-3 text-white/75">
                <motion.span
                  className="h-3 w-3 rounded-full bg-white"
                  animate={{ opacity: [0.4, 1, 0.4] }}
                  transition={{ duration: 1.2, repeat: Infinity }}
                />
                <span className="text-xs font-light tracking-[0.22em]">
                  REC
                </span>
              </div>
              <div className="flex items-center gap-2 text-white/45">
                <Mic className="h-4 w-4" strokeWidth={1.5} />
                <span className="text-xs font-light tracking-[0.18em]">
                  YOUR TURN
                </span>
              </div>
            </div>
            <p className="text-lg leading-relaxed font-light text-white/60">
              추천 응답
            </p>
            <p className="mt-3 text-3xl leading-snug font-extralight tracking-wide">
              “{mockExperience.userReply}”
            </p>
            <div className="mt-8 grid grid-cols-3 gap-3">
              <MiniScore label="길이" value="적절" />
              <MiniScore label="톤" value="차분" />
              <MiniScore label="시작" value="자연" />
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}

function MiniScore({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-center">
      <p className="text-xs font-light tracking-[0.18em] text-white/40">
        {label}
      </p>
      <p className="mt-1 text-lg font-light text-white/80">{value}</p>
    </div>
  );
}
