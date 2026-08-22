"use client";

import { motion } from "framer-motion";

export function ScanningEffect() {
  return (
    <div className="relative h-[clamp(140px,32vh,500px)] w-[clamp(120px,20vh,300px)] overflow-hidden">
      {/* Scanning Line */}
      <motion.div
        className="absolute right-0 left-0 h-[2px]"
        style={{
          background:
            "linear-gradient(90deg, transparent, rgba(255,255,255,0.6), transparent)",
          boxShadow:
            "0 0 20px rgba(255,255,255,0.3), 0 0 40px rgba(255,255,255,0.1)",
        }}
        animate={{
          top: ["0%", "100%", "0%"],
        }}
        transition={{
          duration: 4,
          repeat: Infinity,
          ease: "easeInOut",
        }}
      />

      {/* Subtle Grid Overlay */}
      <motion.div
        className="absolute inset-0 opacity-10"
        style={{
          backgroundImage: `
            linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px),
            linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px)
          `,
          backgroundSize: "20px 20px",
        }}
        animate={{
          opacity: [0.05, 0.15, 0.05],
        }}
        transition={{
          duration: 2,
          repeat: Infinity,
          ease: "easeInOut",
        }}
      />

      {/* Face Detection Frame */}
      <motion.div
        className="absolute top-[15%] left-1/2 h-28 w-24 -translate-x-1/2"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5 }}
      >
        {/* Corner brackets */}
        <div className="absolute top-0 left-0 h-4 w-4 border-t border-l border-white/30" />
        <div className="absolute top-0 right-0 h-4 w-4 border-t border-r border-white/30" />
        <div className="absolute bottom-0 left-0 h-4 w-4 border-b border-l border-white/30" />
        <div className="absolute right-0 bottom-0 h-4 w-4 border-r border-b border-white/30" />
      </motion.div>
    </div>
  );
}
