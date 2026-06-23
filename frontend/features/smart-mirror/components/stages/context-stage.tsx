"use client";

import { motion } from "framer-motion";
import type { ReactNode } from "react";
import { Mic } from "lucide-react";
import { mockExperience } from "../../data/mock-experience";
import { ScanningEffect } from "../shared/scanning-effect";
import { SttPanel } from "../shared/stt-panel";

export function ContextStage() {
  return (
    <div className="relative h-full px-8">
      <div className="absolute inset-0 flex items-center justify-center">
        <ScanningEffect />
      </div>
      <motion.div
        className="absolute top-32 left-10 z-20 flex max-w-xs flex-col gap-3"
        initial="hidden"
        animate="visible"
        variants={{
          hidden: {},
          visible: { transition: { staggerChildren: 0.16 } },
        }}
      >
        <FloatingLabel>파악한 맥락</FloatingLabel>
        <div className="flex flex-wrap gap-2">
          {mockExperience.tags.map((tag) => (
            <Tag key={tag} label={tag} />
          ))}
        </div>
      </motion.div>
      <motion.div
        className="absolute top-32 right-10 z-20 flex max-w-xs flex-col gap-3"
        initial="hidden"
        animate="visible"
        variants={{
          hidden: {},
          visible: {
            transition: { staggerChildren: 0.18, delayChildren: 0.3 },
          },
        }}
      >
        <FloatingLabel>더 필요한 맥락</FloatingLabel>
        <div className="flex flex-col gap-2">
          {mockExperience.missing.map((label) => (
            <MissingTag key={label} label={label} />
          ))}
        </div>
      </motion.div>
      <div className="relative z-10 flex h-full flex-col items-center justify-center px-8 pt-32 pb-28">
        <motion.div
          className="flex max-w-4xl flex-col items-center gap-5 text-center"
          initial={{ opacity: 0, y: -14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7 }}
        >
          <div className="flex items-center gap-2 rounded-full border border-white/15 bg-white/8 px-4 py-2 text-white/75 backdrop-blur-xl">
            <Mic className="h-4 w-4" strokeWidth={1.5} />
            <span className="text-xs font-light tracking-[0.22em]">
              ANSWER OUT LOUD
            </span>
          </div>
          <h1 className="text-4xl font-extralight tracking-wide md:text-6xl">
            조금만 더 알려주세요
          </h1>
          <p className="max-w-2xl text-base font-light text-white/65 md:text-lg">
            내일의 장면을 완성하려면 세 가지가 더 필요해요. 아래 질문에 이어서
            말해주세요.
          </p>
          <div className="mt-4 grid w-full grid-cols-3 gap-3">
            {mockExperience.followUpQuestions.map((question, index) => (
              <motion.div
                key={question}
                className="rounded-2xl border border-white/10 bg-black/25 px-5 py-4 text-left backdrop-blur-xl"
                initial={{ opacity: 0, y: 14 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.18 + index * 0.1, duration: 0.45 }}
              >
                <p className="mb-2 text-xs font-light tracking-[0.2em] text-white/45">
                  QUESTION {index + 1}
                </p>
                <p className="text-lg leading-snug font-light text-white/85">
                  {question}
                </p>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>
      <div className="absolute inset-x-0 bottom-12 z-20 flex justify-center px-6">
        <SttPanel
          text={mockExperience.contextReply}
          label="FOLLOW-UP STT"
          compact
        />
      </div>
    </div>
  );
}

function FloatingLabel({ children }: { children: ReactNode }) {
  return (
    <p className="text-xs font-light tracking-[0.22em] text-white/45">
      {children}
    </p>
  );
}

function Tag({ label }: { label: string }) {
  return (
    <motion.div
      className="rounded-full border border-white/20 bg-white/8 px-4 py-2 text-sm font-light text-white/85 backdrop-blur-xl"
      variants={{
        hidden: { opacity: 0, y: 14, scale: 0.92 },
        visible: { opacity: 1, y: 0, scale: 1 },
      }}
    >
      {label}
    </motion.div>
  );
}

function MissingTag({ label }: { label: string }) {
  return (
    <motion.div
      className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm font-light text-white/55 backdrop-blur-xl"
      variants={{
        hidden: { opacity: 0, y: 14, scale: 0.92 },
        visible: { opacity: 1, y: 0, scale: 1 },
      }}
    >
      {label} 필요
    </motion.div>
  );
}
