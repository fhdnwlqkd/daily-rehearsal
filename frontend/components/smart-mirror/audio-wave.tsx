"use client"

import { motion } from "framer-motion"

export function AudioWave() {
  const bars = 24
  
  return (
    <div className="flex h-16 items-center justify-center gap-1">
      {Array.from({ length: bars }).map((_, i) => {
        const delay = i * 0.05
        const centerDistance = Math.abs(i - bars / 2)
        const baseHeight = Math.max(8, 40 - centerDistance * 3)
        
        return (
          <motion.div
            key={i}
            className="w-[2px] rounded-full bg-gradient-to-t from-white/20 to-white/60"
            animate={{
              height: [
                baseHeight * 0.3,
                baseHeight,
                baseHeight * 0.5,
                baseHeight * 0.8,
                baseHeight * 0.3,
              ],
            }}
            transition={{
              duration: 1.5,
              repeat: Infinity,
              delay,
              ease: "easeInOut",
            }}
          />
        )
      })}
    </div>
  )
}
