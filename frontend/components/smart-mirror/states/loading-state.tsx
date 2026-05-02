"use client"

import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { GlassPanel } from "../glass-panel"
import { ScanningEffect } from "../scanning-effect"

const loadingMessages = [
  "기상 및 환경 데이터 동기화 중...",
  "일정 리스크 분석 및 스타일 도출 중...",
  "최적의 라이프스타일 렌더링 중...",
]

export function LoadingState() {
  const [messageIndex, setMessageIndex] = useState(0)

  useEffect(() => {
    const interval = setInterval(() => {
      setMessageIndex((prev) => (prev + 1) % loadingMessages.length)
    }, 1500)

    return () => clearInterval(interval)
  }, [])

  return (
    <div className="relative flex h-full w-full flex-col">
      {/* Center: Scanning Effect */}
      <div className="absolute inset-0 flex items-center justify-center">
        <ScanningEffect />
      </div>

      {/* Bottom Panel */}
      <div className="absolute inset-x-0 bottom-0 flex justify-center px-6 pb-12">
        <GlassPanel>
          <AnimatePresence mode="wait">
            <motion.p
              key={messageIndex}
              className="min-w-[320px] text-center font-extralight tracking-wide text-white/90"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.4, ease: "easeInOut" }}
            >
              {loadingMessages[messageIndex]}
            </motion.p>
          </AnimatePresence>
        </GlassPanel>
      </div>
    </div>
  )
}
