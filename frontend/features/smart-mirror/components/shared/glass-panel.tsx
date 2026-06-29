"use client";

import { motion } from "framer-motion";
import { cn } from "@/lib/utils";

interface GlassPanelProps {
  children: React.ReactNode;
  className?: string;
  pulsing?: boolean;
  pulseColor?: string;
}

export function GlassPanel({
  children,
  className,
  pulsing = false,
  pulseColor = "rgba(134, 239, 172, 0.4)",
}: GlassPanelProps) {
  return (
    <motion.div
      className={cn(
        "relative rounded-2xl border border-white/10 bg-white/5 px-8 py-6 backdrop-blur-2xl",
        className,
      )}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: "easeOut" }}
    >
      {pulsing && (
        <motion.div
          className="absolute inset-0 rounded-2xl border"
          style={{ borderColor: pulseColor }}
          animate={{
            opacity: [0.3, 0.8, 0.3],
          }}
          transition={{
            duration: 2,
            repeat: Infinity,
            ease: "easeInOut",
          }}
        />
      )}
      {children}
    </motion.div>
  );
}
