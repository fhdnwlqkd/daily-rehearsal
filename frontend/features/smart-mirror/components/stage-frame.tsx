"use client";

import { motion } from "framer-motion";
import type { ReactNode } from "react";
import type { ExperiencePhase, ExperiencePhaseId } from "../types";

interface StageFrameProps {
  phase: ExperiencePhase;
  phaseIndex: number;
  totalPhases: number;
  /** 현재 스테이지 화면. 어떤 스테이지를 그릴지·props 주입은 세션 층(ExperienceSession) 책임. */
  children: ReactNode;
}

/**
 * 모든 스테이지가 공유하는 공통 프레임 — 톤 오버레이·헤더(진행 표시)·
 * 전환 연출·조작 힌트를 씌우고 받은 스테이지(children)를 그린다.
 * REC 인디케이터처럼 여러 스테이지에 걸치는 표시도 이 레벨에 둔다.
 */
export function StageFrame({
  phase,
  phaseIndex,
  totalPhases,
  children,
}: StageFrameProps) {
  // 티켓은 웹캠을 쓰지 않되, 공용 헤더·진행 표시·조작 힌트는 유지한다.
  // 결과 화면만 불투명하게 덮어 앞선 스테이지와 같은 제품 문법을 이어간다.
  if (phase.id === "ticket") {
    return (
      <div className="relative h-full w-full overflow-hidden bg-[#e9eef1] text-[#172027]">
        {children}
        <TapHint phase={phase.id} />
      </div>
    );
  }

  return (
    <div className="relative h-full w-full overflow-hidden text-white">
      <MirrorToneOverlay phase={phase.id} />
      <StageHeader
        phase={phase}
        phaseIndex={phaseIndex}
        totalPhases={totalPhases}
      />
      {phase.id !== "type-select" && <TransitionCue phase={phase.id} />}

      {children}

      <TapHint phase={phase.id} />
    </div>
  );
}

function MirrorToneOverlay({ phase }: { phase: ExperiencePhaseId }) {
  // 타입 선택은 화면 중앙에 제목·카드·힌트가 몰리는데 via가 투명하면
  // 생영상 위에 글자가 얹혀 전시 조명에 씻긴다 — 중앙에도 스크림을 깐다.
  const overlays: Record<ExperiencePhaseId, string> = {
    "type-select": "from-black/40 via-black/20 to-black/65",
    briefing: "from-black/55 via-black/10 to-black/75",
    outfit: "from-black/45 via-black/10 to-black/75",
    simulation: "from-black/45 via-black/20 to-black/80",
    ticket: "from-black/70 via-black/55 to-black/90",
  };

  return (
    <>
      <div className={`absolute inset-0 bg-gradient-to-b ${overlays[phase]}`} />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,transparent_0%,rgba(0,0,0,0.18)_54%,rgba(0,0,0,0.7)_100%)]" />
    </>
  );
}

function StageHeader({
  phase,
  phaseIndex,
  totalPhases,
}: {
  phase: ExperiencePhase;
  phaseIndex: number;
  totalPhases: number;
}) {
  const isTicket = phase.id === "ticket";

  return (
    // iframe 최소 폭(280px)에서도 로고·라벨·진행바가 한 줄을 유지해야 한다(#232) —
    // 줄바꿈 대신 라벨을 숨기고 자간·바 폭을 줄인다.
    <div className="absolute top-[clamp(12px,2.5vh,24px)] right-[clamp(14px,3vw,32px)] left-[clamp(14px,3vw,32px)] z-20 flex items-center justify-between gap-3">
      <span
        className={`text-xs font-light tracking-[0.32em] whitespace-nowrap max-[480px]:tracking-[0.18em] ${isTicket ? "text-[#40515b]" : "text-white/70"}`}
      >
        DAILY REHEARSAL
      </span>
      <div className="flex items-center gap-3">
        <span
          className={`text-xs font-light tracking-[0.16em] whitespace-nowrap max-[480px]:hidden ${isTicket ? "text-[#60707a]" : "text-white/60"}`}
        >
          {phase.label}
        </span>
        <div className="flex gap-1.5">
          {Array.from({ length: totalPhases }).map((_, index) => (
            <div
              key={index}
              className={`h-1.5 w-[clamp(14px,2.6vw,28px)] rounded-full ${
                index <= phaseIndex
                  ? isTicket
                    ? "bg-[#00B0F0]"
                    : "bg-white/70"
                  : isTicket
                    ? "bg-[#cbd5da]"
                    : "bg-white/15"
              }`}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

function TransitionCue({ phase }: { phase: ExperiencePhaseId }) {
  const copy: Record<ExperiencePhaseId, string> = {
    "type-select": "",
    briefing: "내일의 상황을 그려봅니다",
    outfit: "내일의 모습을 입혀봅니다",
    simulation: "결정적 순간을 연습합니다",
    ticket: "내일의 티켓을 발급합니다",
  };

  return (
    <motion.div
      className="pointer-events-none absolute inset-0 z-40 flex items-center justify-center bg-black/35 backdrop-blur-[2px]"
      initial={{ opacity: 0 }}
      animate={{ opacity: [0, 1, 1, 0] }}
      transition={{
        duration: 2.4,
        times: [0, 0.16, 0.72, 1],
        ease: "easeInOut",
      }}
    >
      <motion.div
        className="border-y border-white/20 px-[clamp(1.25rem,5vw,2.5rem)] py-[clamp(1.25rem,4vh,2rem)] text-center"
        initial={{ y: 18, scale: 0.98 }}
        animate={{ y: [18, 0, 0, -10], scale: [0.98, 1, 1, 0.99] }}
        transition={{
          duration: 2.4,
          times: [0, 0.16, 0.72, 1],
          ease: "easeInOut",
        }}
      >
        <p className="mb-[clamp(0.5rem,1.5vh,1rem)] text-xs font-light tracking-[0.34em] text-white/55">
          NEXT SEQUENCE
        </p>
        <p className="text-[clamp(1.5rem,6vw,4.5rem)] font-extralight tracking-wide break-keep text-white">
          {copy[phase]}
        </p>
      </motion.div>
    </motion.div>
  );
}

function TapHint({ phase }: { phase: ExperiencePhaseId }) {
  // 타입 선택의 제스처 안내는 스테이지 안(GestureHint)에서 상황별로 보여주므로
  // 여기서는 키보드 대체 입력만 알린다. 나머지는 개발용 진행 힌트.
  const copy: Record<ExperiencePhaseId, string> = {
    "type-select": "키보드 ←/→·Enter로도 조작할 수 있어요",
    briefing: "마이크에 대고 답해주세요 · Enter 바로 전송 · ← 다시 말하기",
    outfit: "키보드 ←/→·Enter로도 조작할 수 있어요",
    simulation: "마이크에 대고 답해주세요 · Enter 바로 전송 · ← 다시 말하기",
    ticket: "탭하거나 Enter를 눌러 다시 시작",
  };
  // 세로 박스(모바일 iframe, #232)에는 키보드가 없다 — 키보드 언급을 뺀 짧은
  // 문구로 바꾸고, 키보드 안내뿐인 스테이지에서는 힌트 자체를 숨긴다.
  const portraitCopy: Record<ExperiencePhaseId, string> = {
    "type-select": "",
    briefing: "마이크에 대고 답해주세요",
    outfit: "",
    simulation: "마이크에 대고 답해주세요",
    ticket: "탭해서 다시 시작",
  };

  return (
    <motion.div
      className={`absolute bottom-[clamp(10px,2vh,24px)] left-1/2 z-30 w-max max-w-[92%] -translate-x-1/2 rounded-full px-5 py-2 text-center font-light tracking-[0.2em] backdrop-blur-sm ${
        phase === "ticket"
          ? "border border-[#cbd5da] bg-white/90 text-base text-[#4d5e68] shadow-sm"
          : "bg-black/40 text-sm text-white/80"
      } ${portraitCopy[phase] ? "" : "portrait:hidden"} [@media(orientation:landscape)_and_(max-height:400px)]:hidden`}
      // 티켓은 밝은 배경 위 콘텐츠와 겹칠 수 있어 반투명 깜빡임 대신 거의
      // 불투명하게 유지한다 — 떠 있는 버튼처럼 읽히게.
      animate={{
        opacity: phase === "ticket" ? [0.88, 1, 0.88] : [0.45, 0.9, 0.45],
      }}
      transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
    >
      <span className="portrait:hidden">{copy[phase]}</span>
      {portraitCopy[phase] && (
        <span className="hidden portrait:inline">{portraitCopy[phase]}</span>
      )}
    </motion.div>
  );
}
