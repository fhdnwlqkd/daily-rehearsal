"use client";

import { useCallback, useEffect, useState } from "react";
import { motion } from "framer-motion";
import { GlassPanel } from "../shared/glass-panel";
import { useGetSituationTypes } from "../../hooks/use-get-situation-types";
import { useCreateSession } from "../../hooks/use-create-session";
import { useGestureController } from "../../hooks/use-gesture-controller";
import type {
  CreateSessionResponse,
  GestureActionEvent,
  GestureEngineHandle,
  SituationType,
} from "../../types";

interface TypeSelectStageProps {
  /** 부스 수명 장비 — 스테이지는 소비만 한다. */
  engine: GestureEngineHandle;
  stream: MediaStream | null;
  /** 세션 생성 성공 시 세션 층으로 올린다 — 스테이지는 세션 상태를 소유하지 않는다. */
  onSessionCreated: (
    session: CreateSessionResponse,
    situationType: SituationType,
  ) => void;
}

/**
 * 1. 타입 선택 — 연습할 상황 타입을 고르고 세션을 생성한다.
 * 입력은 useGestureController 하나로 들어온다: 손 스와이프(NEXT/PREV)로
 * 하이라이트 이동, 팜홀드(CONFIRM)로 확정. 키보드(←/→·Enter)는 컨트롤러에
 * 내장된 병렬 입력(운영자 개입 통로)이라 별도 처리가 없다.
 */
export function TypeSelectStage({
  engine,
  stream,
  onSessionCreated,
}: TypeSelectStageProps) {
  const { situationTypes, status: listStatus } = useGetSituationTypes();
  const { session, status: createStatus, create } = useCreateSession();
  const [highlightIndex, setHighlightIndex] = useState(0);

  const confirm = useCallback(() => {
    const highlighted = situationTypes[highlightIndex];
    // LOADING(중복 요청)·READY(이미 성공) 중에는 확정을 막는다. ERROR는 재시도 허용.
    if (!highlighted || createStatus === "LOADING" || createStatus === "READY")
      return;
    create(highlighted.key);
  }, [situationTypes, highlightIndex, createStatus, create]);

  const handleAction = useCallback(
    (event: GestureActionEvent) => {
      if (listStatus !== "READY") return;

      if (event.action === "NEXT") {
        setHighlightIndex((index) =>
          Math.min(situationTypes.length - 1, index + 1),
        );
      }
      if (event.action === "PREV") {
        setHighlightIndex((index) => Math.max(0, index - 1));
      }
      if (event.action === "CONFIRM") {
        confirm();
      }
    },
    [listStatus, situationTypes.length, confirm],
  );

  const {
    status: gestureStatus,
    handVisible,
    confirmProgress,
  } = useGestureController({
    engine,
    stream,
    onAction: handleAction,
    // 세션 생성 성공 후에는 인식 루프·키 입력을 정지해 이중 확정을 막는다.
    enabled: createStatus !== "READY",
  });

  // 세션 생성 성공 → 선택된 타입과 함께 세션 층으로 올린다.
  // (하이라이트가 아니라 응답의 situationType으로 역참조 — 성공 후
  // 하이라이트가 움직여도 올라가는 타입이 흔들리지 않는다.)
  useEffect(() => {
    if (createStatus !== "READY" || !session) return;

    const selected = situationTypes.find(
      (type) => type.key === session.situationType,
    );
    if (selected) onSessionCreated(session, selected);
  }, [createStatus, session, situationTypes, onSessionCreated]);

  return (
    <div className="flex h-full flex-col items-center justify-center gap-12 px-8">
      <div className="text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]">
        <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/65">
          SELECT SITUATION
        </p>
        <h2 className="text-4xl font-extralight tracking-wide md:text-5xl">
          내일 연습할 상황을 골라주세요
        </h2>
      </div>

      {listStatus === "LOADING" && (
        <StatusLine text="상황 목록을 불러오는 중…" />
      )}
      {listStatus === "ERROR" && (
        <StatusLine text="상황 목록을 불러오지 못했습니다" error />
      )}

      {listStatus === "READY" && (
        <div className="flex flex-wrap justify-center gap-6">
          {situationTypes.map((type, index) => (
            <TypeCard
              key={type.key}
              type={type}
              highlighted={index === highlightIndex}
              confirmProgress={index === highlightIndex ? confirmProgress : 0}
            />
          ))}
        </div>
      )}

      {/* 안내 슬롯 — 상태가 바뀌어도 높이를 고정해 카드가 출렁이지 않게 한다 */}
      <div className="flex h-28 items-center justify-center">
        {createStatus === "IDLE" && listStatus === "READY" && (
          <GestureHint
            gestureStatus={gestureStatus}
            handVisible={handVisible}
            confirmProgress={confirmProgress}
            highlightedLabel={situationTypes[highlightIndex]?.label ?? null}
          />
        )}
        {createStatus === "LOADING" && (
          <StatusLine text="세션을 준비하는 중…" />
        )}
        {createStatus === "ERROR" && (
          <StatusLine
            text="세션 생성에 실패했습니다 — 손바닥을 펴거나 Enter로 다시 시도"
            error
          />
        )}
      </div>
    </div>
  );
}

/**
 * 제스처 안내 — 전시 관람객은 텍스트를 읽지 않고 그림을 따라 하므로
 * 상태별 픽토그램 애니메이션이 1차 안내, 텍스트는 보조다.
 * 상태 전환 자체가 "인식되고 있다"는 피드백 역할을 한다.
 */
function GestureHint({
  gestureStatus,
  handVisible,
  confirmProgress,
  highlightedLabel,
}: {
  gestureStatus: GestureEngineHandle["status"];
  handVisible: boolean;
  confirmProgress: number;
  highlightedLabel: string | null;
}) {
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
        <div className="flex items-center gap-3 rounded-2xl bg-black/35 px-6 py-3 backdrop-blur-sm">
          <PalmEmoji className="text-2xl" />
          <div className="flex flex-col items-start gap-0.5">
            <span className="text-sm font-light tracking-[0.15em] text-white/90">
              이대로 멈춰 있으면{" "}
              {highlightedLabel ? `'${highlightedLabel}' ` : ""}선택
            </span>
            <span className="text-xs font-light tracking-[0.15em] text-white/60">
              다른 타입은 ← → 스와이프
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
        <div className="flex items-center gap-3 rounded-full bg-black/35 px-5 py-2.5 backdrop-blur-sm">
          <span className="text-base text-white/50">←</span>
          <motion.div
            animate={{ x: [-8, 8, -8] }}
            transition={{ duration: 1.6, repeat: Infinity, ease: "easeInOut" }}
          >
            <PalmEmoji className="text-2xl" />
          </motion.div>
          <span className="text-base text-white/50">→</span>
          <span className="text-sm font-light tracking-[0.15em] text-white/85">
            스와이프로 타입 변경
          </span>
        </div>
        <div className="flex items-center gap-3 rounded-full bg-black/35 px-5 py-2.5 backdrop-blur-sm">
          <span className="relative inline-flex h-10 w-10 items-center justify-center">
            <PalmEmoji className="text-2xl" />
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
          <span className="text-sm font-light tracking-[0.15em] text-white/85">
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

function TypeCard({
  type,
  highlighted,
  confirmProgress,
}: {
  type: SituationType;
  highlighted: boolean;
  /** 0~1 팜홀드 진행률 — 하이라이트 카드에만 차오른다. */
  confirmProgress: number;
}) {
  const charging = confirmProgress > 0;

  return (
    // 밝은 영상 위에서는 선택 카드를 키우는 것보다 비선택 카드를 죽이는 게
    // 멀리서도 확실하다. GlassPanel이 framer로 transform을 소유하므로
    // scale/opacity는 래퍼에서 준다.
    <div
      className={`transition-all duration-300 ${highlighted ? "" : "scale-95 opacity-50"}`}
    >
      <GlassPanel
        className={
          highlighted ? "border-white/60 bg-white/20" : "border-white/10"
        }
        pulsing={highlighted}
        pulseColor="rgba(255, 255, 255, 0.35)"
      >
        <div className="flex w-56 flex-col items-center gap-3 text-center">
          <span className="text-xs font-light tracking-[0.3em] text-white/50">
            {String(type.gestureOrder).padStart(2, "0")}
          </span>
          <span className="text-2xl font-extralight tracking-wide">
            {type.label}
          </span>
          {/* 팜홀드 차징 바 — 평소엔 투명해서 구분선처럼 안 보이고,
              차오를 때만 트랙과 함께 나타난다 (레이아웃 시프트 없음) */}
          <div
            className={`h-1 w-full overflow-hidden rounded-full transition-colors ${charging ? "bg-white/10" : "bg-transparent"}`}
          >
            <div
              className="h-full rounded-full bg-white/80 transition-[width] duration-100"
              style={{ width: `${confirmProgress * 100}%` }}
            />
          </div>
        </div>
      </GlassPanel>
    </div>
  );
}

function StatusLine({
  text,
  error = false,
}: {
  text: string;
  error?: boolean;
}) {
  // 행동 지시 텍스트 — 밝은 영상 위에서도 읽히도록 필 스크림을 깐다.
  return (
    <p
      className={`rounded-full bg-black/35 px-5 py-2 text-sm font-light tracking-[0.2em] backdrop-blur-sm ${error ? "text-red-300/90" : "text-white/80"}`}
    >
      {text}
    </p>
  );
}
