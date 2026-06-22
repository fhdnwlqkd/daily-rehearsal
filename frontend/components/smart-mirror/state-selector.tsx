"use client";

import { motion } from "framer-motion";
import type { MirrorState } from "./smart-mirror";
import { experiencePhases } from "./p1-experience";

interface StateSelectorProps {
  currentState: MirrorState;
  onStateChange: (state: MirrorState) => void;
}

export function StateSelector({
  currentState,
  onStateChange,
}: StateSelectorProps) {
  return (
    <div className="absolute top-6 right-6 z-50 flex gap-2">
      {experiencePhases.map((phase, index) => (
        <motion.button
          key={phase.id}
          onClick={() => onStateChange(phase.id)}
          className={`flex h-10 w-10 items-center justify-center rounded-xl border backdrop-blur-xl transition-all ${
            currentState === phase.id
              ? "border-white/40 bg-white/20 text-white"
              : "border-white/10 bg-white/5 text-white/60 hover:border-white/20 hover:bg-white/10 hover:text-white/80"
          }`}
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
        >
          <span className="text-sm font-extralight tracking-wide">
            {index + 1}
          </span>
        </motion.button>
      ))}
    </div>
  );
}
