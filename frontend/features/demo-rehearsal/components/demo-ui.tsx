"use client";

import { motion } from "framer-motion";
import type { ReactNode } from "react";

export function DemoGlassPanel({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={`rounded-3xl border border-white/20 bg-black/35 backdrop-blur-xl ${className}`}
    >
      {children}
    </div>
  );
}

export function DemoStatus({ text }: { text: string }) {
  return (
    <motion.p
      className="text-center text-[clamp(1rem,2vw,1.25rem)] font-extralight tracking-wide text-white/80"
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
    >
      {text}
    </motion.p>
  );
}

export function DemoScanning() {
  return (
    <div className="relative h-20 w-20 overflow-hidden rounded-full border border-white/20">
      <motion.div
        className="absolute right-2 left-2 h-px bg-white/90 shadow-[0_0_14px_rgba(255,255,255,0.8)]"
        animate={{ top: [12, 66, 12] }}
        transition={{ duration: 1.8, repeat: Infinity, ease: "easeInOut" }}
      />
      <div className="absolute inset-3 rounded-full border border-white/10" />
    </div>
  );
}

export function DemoTranscript({
  text,
  label,
}: {
  text: string;
  label: string;
}) {
  return (
    <DemoGlassPanel className="w-full max-w-3xl px-8 py-6 text-center">
      <p className="text-xs font-light tracking-[0.32em] text-white/50">
        {label}
      </p>
      <p className="mt-3 text-[clamp(1.125rem,2.4vw,1.5rem)] leading-relaxed font-extralight break-keep text-white/95">
        “{text}”
      </p>
    </DemoGlassPanel>
  );
}

export function DemoCenter({ children }: { children: ReactNode }) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-6 px-8 pt-16 pb-20">
      {children}
    </div>
  );
}
