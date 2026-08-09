"use client";

import { useEffect, useState } from "react";
import { motion } from "framer-motion";

import { SttPanel } from "./stt-panel";
import { StatusLine } from "./status-line";
import type { UseSpeechToTextResult } from "../../hooks/use-speech-to-text";

export type AnswerInputMode = "VOICE" | "KEYBOARD";

interface AnswerAreaProps {
  inputMode: AnswerInputMode;
  stt: UseSpeechToTextResult;
  /** 키보드로 타이핑된 답변 — 음성 확정(onConfirm)과 같은 제출 경로로 합류시킬 것. */
  onSubmitTyped: (text: string) => void;
  /** 확정 대기 패널의 라벨 (예: "YOUR BRIEFING", "YOUR ANSWER"). */
  sttLabel: string;
  typedPlaceholder: string;
  /** CANDIDATE 진입 후 자동 전송까지의 카운트다운 (CONFIRM은 즉시, PREV는 취소). */
  autoConfirmMs: number;
}

/**
 * 음성/키보드 답변 입력 영역 — 브리핑·시뮬레이션이 함께 쓰는 공용 부품.
 * 높이를 고정해 상태 전환 때 위 내용(질문·상대 발화)이 출렁이지 않게 한다.
 *
 * CANDIDATE 자동 전송 타이머는 여기가 소유한다(카운트다운 바와 시간이 항상
 * 일치). 마이크 자동 시작·키보드 전환·제스처 배선은 답변 가능 조건이
 * 스테이지마다 달라 스테이지가 소유한다.
 */
export function AnswerArea({
  inputMode,
  stt,
  onSubmitTyped,
  sttLabel,
  typedPlaceholder,
  autoConfirmMs,
}: AnswerAreaProps) {
  const { status: sttStatus, confirm: sttConfirm } = stt;

  // 자동 확정 — CANDIDATE 진입 후 카운트다운이 다 차면 전송
  useEffect(() => {
    if (inputMode !== "VOICE" || sttStatus !== "CANDIDATE") return;
    const timer = setTimeout(() => sttConfirm(), autoConfirmMs);
    return () => clearTimeout(timer);
  }, [inputMode, sttStatus, sttConfirm, autoConfirmMs]);

  return (
    <div className="flex min-h-44 w-full flex-col items-center justify-start gap-4">
      {inputMode === "KEYBOARD" ? (
        <TypedAnswerInput
          placeholder={typedPlaceholder}
          onSubmit={onSubmitTyped}
        />
      ) : (
        <VoiceAnswer stt={stt} label={sttLabel} autoConfirmMs={autoConfirmMs} />
      )}
    </div>
  );
}

function VoiceAnswer({
  stt,
  label,
  autoConfirmMs,
}: {
  stt: UseSpeechToTextResult;
  label: string;
  autoConfirmMs: number;
}) {
  if (stt.status === "ERROR") {
    // 복구 가능 에러만 여기 온다 — 비복구(미지원·권한)와 연속 실패는
    // 스테이지가 키보드 모드로 전환한다.
    return (
      <StatusLine
        text="음성 인식에 실패했습니다 — 손바닥을 펴거나 Enter로 다시 시도"
        error
      />
    );
  }

  if (stt.status === "CANDIDATE") {
    return (
      <>
        <SttPanel text={stt.transcript} label={label} />
        <AutoConfirmCountdown durationMs={autoConfirmMs} />
        <StatusLine text="이대로 전송 · ← 다시 말하기 · Enter 바로 전송" />
      </>
    );
  }

  if (stt.status === "LISTENING" && stt.transcript) {
    return <SttPanel text={stt.transcript} label="LISTENING" />;
  }

  return (
    <FadeIn>
      <div className="flex flex-col items-center gap-2">
        <motion.span
          className="text-5xl"
          animate={{ opacity: [0.5, 1, 0.5] }}
          transition={{ duration: 1.6, repeat: Infinity, ease: "easeInOut" }}
          aria-hidden
        >
          🎙️
        </motion.span>
        <StatusLine text="편하게 이야기해 주세요 — 듣고 있어요" />
      </div>
    </FadeIn>
  );
}

/** CANDIDATE 자동 전송까지 차오르는 카운트다운 바 (타입 선택 차징 바와 같은 문법). */
function AutoConfirmCountdown({ durationMs }: { durationMs: number }) {
  return (
    <div className="h-1 w-full max-w-3xl overflow-hidden rounded-full bg-white/10">
      <motion.div
        className="h-full rounded-full bg-white/80"
        initial={{ width: "0%" }}
        animate={{ width: "100%" }}
        transition={{ duration: durationMs / 1000, ease: "linear" }}
      />
    </div>
  );
}

/** STT 불가 시의 키보드 대체 입력 — 타이핑된 텍스트도 같은 제출 경로로 합류한다. */
function TypedAnswerInput({
  placeholder,
  onSubmit,
}: {
  placeholder: string;
  onSubmit: (text: string) => void;
}) {
  const [text, setText] = useState("");

  return (
    <div className="flex w-full max-w-3xl flex-col items-center gap-3">
      <input
        autoFocus
        value={text}
        onChange={(event) => setText(event.target.value)}
        onKeyDown={(event) => {
          if (event.key !== "Enter" || event.nativeEvent.isComposing) return;
          onSubmit(text);
        }}
        placeholder={placeholder}
        className="w-full rounded-2xl border border-white/20 bg-black/40 px-6 py-4 text-center text-xl font-extralight tracking-wide text-white/90 backdrop-blur-xl placeholder:text-white/35 focus:border-white/50 focus:outline-none"
      />
      <StatusLine text="음성 인식을 사용할 수 없어 키보드로 입력합니다 — Enter로 전송" />
    </div>
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
