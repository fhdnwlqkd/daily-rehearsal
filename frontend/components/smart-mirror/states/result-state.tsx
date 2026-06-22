"use client";

import { motion } from "framer-motion";
import { ChevronLeft, ChevronRight, CloudRain } from "lucide-react";
import { GlassPanel } from "../glass-panel";

export function ResultState() {
  return (
    <div className="relative flex h-full w-full flex-col">
      {/* Side Navigation Arrows */}
      <div className="pointer-events-none absolute inset-0 flex items-center justify-between px-8">
        <motion.button
          className="pointer-events-auto rounded-full p-4"
          animate={{
            opacity: [0.3, 0.7, 0.3],
          }}
          transition={{
            duration: 3,
            repeat: Infinity,
            ease: "easeInOut",
          }}
          whileHover={{ scale: 1.1, opacity: 1 }}
          whileTap={{ scale: 0.95 }}
        >
          <ChevronLeft className="h-8 w-8 text-white/60" strokeWidth={1} />
        </motion.button>
        <motion.button
          className="pointer-events-auto rounded-full p-4"
          animate={{
            opacity: [0.3, 0.7, 0.3],
          }}
          transition={{
            duration: 3,
            repeat: Infinity,
            ease: "easeInOut",
            delay: 0.5,
          }}
          whileHover={{ scale: 1.1, opacity: 1 }}
          whileTap={{ scale: 0.95 }}
        >
          <ChevronRight className="h-8 w-8 text-white/60" strokeWidth={1} />
        </motion.button>
      </div>

      {/* Virtual Fitting Overlay Indicator */}
      <motion.div
        className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2"
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.8, ease: "easeOut" }}
      >
        <div className="relative h-[400px] w-[250px]">
          {/* Subtle fitting frame */}
          <motion.div
            className="absolute inset-0 rounded-3xl border border-white/10"
            animate={{
              borderColor: [
                "rgba(255,255,255,0.1)",
                "rgba(255,255,255,0.2)",
                "rgba(255,255,255,0.1)",
              ],
            }}
            transition={{
              duration: 3,
              repeat: Infinity,
              ease: "easeInOut",
            }}
          />
          {/* Success indicator */}
          <motion.div
            className="absolute -top-2 -right-2 flex h-6 w-6 items-center justify-center rounded-full bg-emerald-500/80 backdrop-blur-sm"
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.5, type: "spring" }}
          >
            <svg
              className="h-3 w-3 text-white"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={2}
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M5 13l4 4L19 7"
              />
            </svg>
          </motion.div>
        </div>
      </motion.div>

      {/* Bottom Panel */}
      <div className="absolute inset-x-0 bottom-0 flex justify-center px-6 pb-12">
        <GlassPanel>
          <div className="flex flex-col items-center gap-3">
            <div className="flex items-center gap-2 text-white/60">
              <CloudRain className="h-4 w-4" strokeWidth={1.5} />
              <span className="text-xs font-extralight tracking-wider">
                오후 3시 강수확률 70%
              </span>
            </div>
            <p className="max-w-md text-center font-extralight tracking-wide text-white/90">
              발수 소재의 비즈니스 캐주얼로 매치했습니다.
            </p>
            <div className="mt-2 flex gap-1">
              {[0, 1, 2].map((i) => (
                <motion.div
                  key={i}
                  className={`h-1 w-6 rounded-full ${i === 1 ? "bg-white/60" : "bg-white/20"}`}
                />
              ))}
            </div>
          </div>
        </GlassPanel>
      </div>
    </div>
  );
}
