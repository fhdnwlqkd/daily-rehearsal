"use client";

import { motion } from "framer-motion";
import type { GestureEngineHandle } from "../../types";
import { StatusLine } from "./status-line";

interface GestureHintProps {
  gestureStatus: GestureEngineHandle["status"];
  handVisible: boolean;
  /** 0~1 팜홀드 진행률. 0보다 크면 확정 예고 상태로 전환한다. */
  confirmProgress: number;
  /** 하이라이트된 항목 라벨 — 확정 예고 문구에 들어간다. */
  highlightedLabel: string | null;
  /** 스와이프 대상 명사 (예: "타입", "옷") — 안내 문구에 들어간다. */
  subject: string;
}

/**
 * 제스처 안내 — 전시 관람객은 텍스트를 읽지 않고 그림을 따라 하므로
 * 상태별 픽토그램 애니메이션이 1차 안내, 텍스트는 보조다.
 * 상태 전환 자체가 "인식되고 있다"는 피드백 역할을 한다.
 * (타입 선택에서 시작해 옷 입히기와 공용 — 문구의 대상만 subject로 바뀐다.)
 */
export function GestureHint({
  gestureStatus,
  handVisible,
  confirmProgress,
  highlightedLabel,
  subject,
}: GestureHintProps) {
  if (gestureStatus === "LOADING") {
    return <StatusLine text="제스처 인식을 준비하는 중…" />;
  }
  if (gestureStatus === "ERROR") {
    return (
      <StatusLine text="제스처 인식을 사용할 수 없습니다 — 키보드(←/→·Enter)로 진행하세요" />
    );
  }
  // State 3: 팜홀드 진행 중 — 확정 예고와 함께 "바꾸는 방법"도 이 자리에서
  // 가르친다. 안내를 따라 손바닥부터 편 관람객이 State 2를 건너뛰어도
  // 스와이프를 인지할 기회가 생긴다 (스와이프하면 홀드는 자동 리셋).
  if (confirmProgress > 0) {
    return (
      <FadeIn>
        {/* 크기·스크림은 StatusLine과 같은 급으로 맞춘다 — 같은 화면에서 안내끼리 크기가 튀지 않게 */}
        <div className="flex items-center gap-4 rounded-2xl bg-black/45 px-7 py-3.5 backdrop-blur-sm">
          <PalmEmoji className="text-3xl" />
          <div className="flex flex-col items-start gap-1">
            <span className="text-base font-light tracking-[0.15em] text-white/90 md:text-lg">
              이대로 멈춰 있으면{" "}
              {highlightedLabel ? `'${highlightedLabel}' ` : ""}선택
            </span>
            <span className="text-sm font-light tracking-[0.15em] text-white/60 md:text-base">
              다른 {subject}은 ← → 스와이프
            </span>
          </div>
        </div>
      </FadeIn>
    );
  }
  // State 1: 손 미감지 — "들어라"를 말이 아니라 떠오르는 모션으로 시연.
  // "손바닥"이라고 하면 확정 제스처를 첫 동작으로 시키는 꼴이라 중립적으로.
  if (!handVisible) {
    return (
      <FadeIn>
        <div className="flex flex-col items-center gap-2">
          <motion.div
            animate={{ y: [10, -8, 10], opacity: [0.6, 1, 0.6] }}
            transition={{ duration: 1.8, repeat: Infinity, ease: "easeInOut" }}
          >
            <PalmEmoji className="text-5xl" />
          </motion.div>
          <StatusLine text="카메라에 손을 들어주세요" />
        </div>
      </FadeIn>
    );
  }
  // State 2: 손 감지됨 — 가능한 두 동작을 움직이는 픽토그램으로 나란히
  return (
    <FadeIn>
      <div className="flex flex-wrap items-center justify-center gap-4">
        <div className="flex items-center gap-3 rounded-full bg-black/45 px-6 py-3 backdrop-blur-sm">
          <span className="text-lg text-white/50">←</span>
          <motion.div
            animate={{ x: [-8, 8, -8] }}
            transition={{ duration: 1.6, repeat: Infinity, ease: "easeInOut" }}
          >
            <PalmEmoji className="text-3xl" />
          </motion.div>
          <span className="text-lg text-white/50">→</span>
          <span className="text-base font-light tracking-[0.15em] text-white/85 md:text-lg">
            스와이프로 {subject} 변경
          </span>
        </div>
        <div className="flex items-center gap-3 rounded-full bg-black/45 px-6 py-3 backdrop-blur-sm">
          <span className="relative inline-flex h-11 w-11 items-center justify-center">
            <PalmEmoji className="text-3xl" />
            <svg
              className="absolute inset-0 h-full w-full -rotate-90"
              viewBox="0 0 40 40"
              aria-hidden
            >
              <motion.circle
                cx="20"
                cy="20"
                r="18"
                fill="none"
                stroke="rgba(255,255,255,0.8)"
                strokeWidth="2.5"
                strokeLinecap="round"
                animate={{ pathLength: [0, 1], opacity: [0.5, 1] }}
                transition={{
                  duration: 1.6,
                  repeat: Infinity,
                  ease: "easeInOut",
                  repeatDelay: 0.5,
                }}
              />
            </svg>
          </span>
          <span className="text-base font-light tracking-[0.15em] text-white/85 md:text-lg">
            잠시 멈추면 선택
          </span>
        </div>
      </div>
    </FadeIn>
  );
}

/** 안내 상태가 바뀔 때 툭 튀지 않게 공통 진입 페이드. */
function FadeIn({ children }: { children: React.ReactNode }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: "easeOut" }}
    >
      {children}
    </motion.div>
  );
}

/** 이모지를 단색 톤으로 눌러 미러 UI 톤과 맞춘다. */
function PalmEmoji({ className = "" }: { className?: string }) {
  return (
    <span
      className={`${className} inline-block drop-shadow-[0_1px_6px_rgba(0,0,0,0.6)] [filter:grayscale(1)_brightness(1.9)]`}
      aria-hidden
    >
      ✋
    </span>
  );
}
