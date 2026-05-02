"use client"

import { motion } from "framer-motion"
import type { MirrorState } from "./smart-mirror"

interface StateSelectorProps {
  currentState: MirrorState
  onStateChange: (state: MirrorState) => void
}

export function StateSelector({ currentState, onStateChange }: StateSelectorProps) {
  const states: MirrorState[] = [1, 2, 3, 4]

  return (
    <div className="absolute top-6 right-6 z-50 flex gap-2">
      {states.map((state) => (
        <motion.button
          key={state}
          onClick={() => onStateChange(state)}
          className={`flex h-10 w-10 items-center justify-center rounded-xl border backdrop-blur-xl transition-all ${
            currentState === state
              ? "border-white/40 bg-white/20 text-white"
              : "border-white/10 bg-white/5 text-white/60 hover:border-white/20 hover:bg-white/10 hover:text-white/80"
          }`}
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
        >
          <span className="text-sm font-extralight tracking-wide">{state}</span>
        </motion.button>
      ))}
    </div>
  )
}
