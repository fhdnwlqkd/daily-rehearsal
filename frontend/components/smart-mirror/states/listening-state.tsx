"use client"

import { motion } from "framer-motion"
import { Mic } from "lucide-react"
import { GlassPanel } from "../glass-panel"
import { AudioWave } from "../audio-wave"

export function ListeningState() {
  return (
    <div className="relative flex h-full w-full flex-col">
      {/* Center: Audio Wave Animation */}
      <div className="flex flex-1 items-center justify-center">
        <AudioWave />
      </div>

      {/* Bottom Panel */}
      <div className="absolute inset-x-0 bottom-0 flex justify-center px-6 pb-12">
        <GlassPanel>
          <div className="flex flex-col items-center gap-4">
            <motion.div
              animate={{
                scale: [1, 1.2, 1],
              }}
              transition={{
                duration: 2,
                repeat: Infinity,
                ease: "easeInOut",
              }}
            >
              <Mic className="h-6 w-6 text-white/70" strokeWidth={1.5} />
            </motion.div>
            <p className="text-center font-extralight tracking-wide text-white/90">
              오늘의 주요 일정은 무엇인가요?
            </p>
            <motion.p
              className="text-center text-sm font-extralight tracking-wide text-white/50"
              animate={{ opacity: [0.3, 0.7, 0.3] }}
              transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
            >
              듣고 있습니다...
            </motion.p>
          </div>
        </GlassPanel>
      </div>
    </div>
  )
}
