"use client";

import { motion } from "framer-motion";
import { AudioWave } from "./audio-wave";
import { GlassPanel } from "./glass-panel";

export function SttPanel({
  text,
  label = "REAL-TIME STT",
  compact = false,
}: {
  text: string;
  label?: string;
  compact?: boolean;
}) {
  return (
    <GlassPanel
      className={`w-full ${compact ? "max-w-3xl px-6 py-4" : "max-w-4xl px-7 py-5"}`}
    >
      <div className="flex items-start gap-5">
        <AudioWave />
        <div className="flex-1">
          <p className="mb-2 text-xs font-light tracking-[0.22em] text-white/50">
            {label}
          </p>
          <RevealText text={text} compact={compact} />
        </div>
      </div>
    </GlassPanel>
  );
}

function RevealText({
  text,
  compact = false,
}: {
  text: string;
  compact?: boolean;
}) {
  const words = text.split(" ");

  return (
    <p
      className={`${compact ? "text-lg" : "text-2xl"} leading-relaxed font-extralight tracking-wide text-white/90`}
    >
      {words.map((word, index) => (
        <motion.span
          key={`${word}-${index}`}
          className="mr-2 inline-block"
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: index * 0.045, duration: 0.25 }}
        >
          {word}
        </motion.span>
      ))}
    </p>
  );
}
