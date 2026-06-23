"use client";

import { motion } from "framer-motion";
import type { ReactNode } from "react";
import {
  ArrowLeft,
  ArrowRight,
  CloudRain,
  MapPin,
  Sparkles,
  UserRound,
} from "lucide-react";
import { mockExperience } from "../../data/mock-experience";

export function TransformationStage() {
  return (
    <div className="relative h-full px-8">
      <GlitchFlash />
      <StyleProjectionOverlay />
      <div className="absolute inset-x-0 top-[16%] z-20 flex flex-col items-center gap-4 text-center">
        <div className="flex items-center gap-2 rounded-full border border-white/15 bg-white/8 px-4 py-2 text-white/75 backdrop-blur-xl">
          <Sparkles className="h-4 w-4" strokeWidth={1.5} />
          <span className="text-xs font-light tracking-[0.22em]">
            VTON PREVIEW
          </span>
        </div>
        <h1 className="text-4xl font-extralight tracking-wide md:text-6xl">
          내일 입어볼 모습을 골라보세요
        </h1>
        <p className="max-w-2xl text-base font-light text-white/65 md:text-lg">
          거울 속 현재 모습 위에 내일의 페르소나 스타일을 겹쳐봅니다.
        </p>
      </div>
      <div className="absolute top-1/2 left-8 z-20 flex -translate-y-1/2 flex-col gap-3">
        <RiskWidget
          icon={<CloudRain className="h-4 w-4" />}
          label="이동 변수"
          value={mockExperience.routeRisk}
          compact
        />
        <RiskWidget
          icon={<MapPin className="h-4 w-4" />}
          label="장소"
          value="성수 음식점"
          compact
        />
      </div>
      <div className="absolute top-1/2 right-8 z-20 flex -translate-y-1/2 flex-col gap-3">
        <RiskWidget
          icon={<UserRound className="h-4 w-4" />}
          label="페르소나"
          value={mockExperience.persona}
          compact
        />
        <RiskWidget
          icon={<MapPin className="h-4 w-4" />}
          label="공간"
          value={mockExperience.placeMood}
          compact
        />
      </div>
      <div className="absolute inset-x-0 bottom-14 z-20 flex justify-center px-8">
        <OutfitCarouselMock />
      </div>
    </div>
  );
}

function GlitchFlash() {
  return (
    <motion.div
      className="pointer-events-none absolute inset-0 z-30 bg-white"
      initial={{ opacity: 0.75 }}
      animate={{ opacity: 0 }}
      transition={{ duration: 0.35, ease: "easeOut" }}
    />
  );
}

function StyleProjectionOverlay() {
  return (
    <div className="pointer-events-none absolute inset-0">
      <motion.div
        className="absolute top-[52%] left-1/2 h-[48vh] w-[min(32vw,340px)] min-w-[250px] -translate-x-1/2 -translate-y-1/2 rounded-[40%_40%_18%_18%] border border-white/25 bg-gradient-to-b from-white/16 via-white/8 to-white/5 shadow-[0_0_90px_rgba(255,255,255,0.12)] backdrop-blur-[1px]"
        initial={{ opacity: 0, scale: 0.9, filter: "blur(12px)" }}
        animate={{ opacity: 1, scale: 1, filter: "blur(0px)" }}
        transition={{ duration: 0.75, ease: "easeOut" }}
      />
      <motion.div
        className="absolute top-[42%] left-1/2 h-[18vh] w-[min(18vw,190px)] min-w-[140px] -translate-x-1/2 -translate-y-1/2 rounded-[45%] border border-white/25"
        animate={{
          borderColor: [
            "rgba(255,255,255,0.18)",
            "rgba(255,255,255,0.52)",
            "rgba(255,255,255,0.18)",
          ],
        }}
        transition={{ duration: 2.2, repeat: Infinity, ease: "easeInOut" }}
      />
      <motion.div
        className="absolute top-[62%] left-1/2 h-px w-[min(48vw,520px)] -translate-x-1/2 bg-gradient-to-r from-transparent via-white/45 to-transparent"
        animate={{ opacity: [0.25, 0.8, 0.25], scaleX: [0.7, 1, 0.7] }}
        transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
      />
    </div>
  );
}

function OutfitCarouselMock() {
  return (
    <div className="w-full max-w-5xl">
      <div className="mb-4 flex items-center justify-center gap-5 text-white/55">
        <ArrowLeft className="h-5 w-5" strokeWidth={1.4} />
        <span className="text-xs font-light tracking-[0.24em]">
          SWIPE TO TRY TOMORROW'S LOOK
        </span>
        <ArrowRight className="h-5 w-5" strokeWidth={1.4} />
      </div>
      <div className="grid grid-cols-3 gap-4">
        {mockExperience.outfits.map((outfit, index) => (
          <motion.div
            key={outfit.name}
            className={`rounded-3xl border px-6 py-5 backdrop-blur-2xl ${
              outfit.active
                ? "border-white/45 bg-white/14 shadow-[0_0_50px_rgba(255,255,255,0.12)]"
                : "border-white/10 bg-black/25"
            }`}
            initial={{ opacity: 0, y: 20 }}
            animate={{
              opacity: outfit.active ? 1 : 0.72,
              y: outfit.active ? [0, -4, 0] : 0,
              scale: outfit.active ? 1.04 : 0.96,
            }}
            transition={{
              delay: index * 0.08,
              duration: outfit.active ? 2 : 0.5,
              repeat: outfit.active ? Infinity : 0,
              ease: "easeInOut",
            }}
          >
            <p className="mb-2 text-xs font-light tracking-[0.22em] text-white/45">
              LOOK {index + 1}
            </p>
            <p className="text-2xl font-extralight tracking-wide text-white">
              {outfit.name}
            </p>
            <p className="mt-2 text-sm font-light text-white/55">
              {outfit.tone}
            </p>
          </motion.div>
        ))}
      </div>
    </div>
  );
}

function RiskWidget({
  icon,
  label,
  value,
  compact = false,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  compact?: boolean;
}) {
  return (
    <motion.div
      className={`${compact ? "w-56 px-4 py-3" : "w-64 px-5 py-4"} rounded-2xl border border-white/10 bg-black/30 backdrop-blur-2xl`}
      initial={{ opacity: 0, x: -18 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.5 }}
    >
      <div className="mb-2 flex items-center gap-2 text-white/50">
        {icon}
        <span className="text-xs font-light tracking-[0.18em]">{label}</span>
      </div>
      <p
        className={`${compact ? "text-lg" : "text-2xl"} font-extralight tracking-wide`}
      >
        {value}
      </p>
    </motion.div>
  );
}
