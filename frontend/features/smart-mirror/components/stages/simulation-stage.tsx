"use client";

import { useCallback, useEffect, useState } from "react";
import { motion } from "framer-motion";
import { AnswerArea } from "../shared/answer-area";
import type { AnswerInputMode } from "../shared/answer-area";
import { GlassPanel } from "../shared/glass-panel";
import { ScanningEffect } from "../shared/scanning-effect";
import { StatusLine } from "../shared/status-line";
import { SttPanel } from "../shared/stt-panel";
import { useGestureController } from "../../hooks/use-gesture-controller";
import { useSimulationFlow } from "../../hooks/use-simulation-flow";
import { useSpeechToText } from "../../hooks/use-speech-to-text";
import {
  SIMULATION_AUTO_CONFIRM_MS,
  SIMULATION_COMPLETE_LINGER_MS,
} from "../../lib/simulation/constants";
import { STT_MAX_FAILS_BEFORE_FALLBACK } from "../../lib/stt/constants";
import type {
  GestureActionEvent,
  GestureEngineHandle,
  SimulationFeedback,
  SimulationFlowFailReason,
} from "../../types";

/** 화면 진입/재발화 후 마이크를 다시 열기까지의 짧은 텀 (전환 연출과 겹침 완화) */
const MIC_START_DELAY_MS = 600;

interface SimulationStageProps {
  /** 세션 층이 소유한 세션 ID — 스테이지는 소비만 한다. */
  sessionId: string;
  /** 부스 수명 장비 — 확정(CONFIRM)/다시 말하기(PREV) 입력용. */
  engine: GestureEngineHandle;
  stream: MediaStream | null;
  /** 전체 턴 성공(COMPLETED) 연출 후 호출 — 세션 층이 티켓으로 넘긴다. */
  onComplete: () => void;
}

/**
 * 4. 시뮬레이션 — 상황 속 대화를 N턴 연습한다 (이슈 #68).
 * 턴 반복은 이 스테이지의 내부 상태다(브리핑의 재질문과 같은 원칙):
 * 턴마다 발화→판정/피드백을 받고, 실패하면 같은 턴을 재시도한다
 * (재시도 무제한 — 기획 확정). maxTurn(성공 횟수) 도달 판정은 프론트 책임.
 * 흐름 상태머신은 SimulationFlowController에 있고, 여기는
 * (flowStatus, sttSnapshot, inputMode)에서 화면을 파생하는 조합만 한다.
 *
 * 상대 발화는 화면 표시만 한다(TTS 없음 — 2026-08-08 기획 확정).
 * 답변 입력은 브리핑과 동일한 공용 AnswerArea(음성 자동 확정 + 키보드 fallback).
 */
export function SimulationStage({
  sessionId,
  engine,
  stream,
  onComplete,
}: SimulationStageProps) {
  const flow = useSimulationFlow(sessionId);
  const [inputMode, setInputMode] = useState<AnswerInputMode>("VOICE");

  const { submitAnswer } = flow;
  // 음성·키보드 공통 제출 합류점. 빈 답변은 보내지 않는다 —
  // STT가 IDLE로 돌아가면 아래 자동 시작 effect가 다시 듣는다.
  const handleAnswer = useCallback(
    (transcript: string) => {
      const trimmed = transcript.trim();
      if (!trimmed) return;
      submitAnswer(trimmed);
    },
    [submitAnswer],
  );

  const stt = useSpeechToText({ onConfirm: handleAnswer });
  const {
    start: sttStart,
    confirm: sttConfirm,
    cancel: sttCancel,
    retry: sttRetry,
  } = stt;

  // 답변을 받을 수 있는 화면인가 — 상대 발화가 표시된 ANSWERING뿐이다.
  const canAnswer = flow.status === "ANSWERING";

  // STT 비복구 에러(미지원·권한)나 연속 실패 → 키보드 전환 (편도 — 세션 내 복귀 없음)
  const shouldFallback =
    stt.errorType === "UNSUPPORTED" ||
    stt.errorType === "PERMISSION" ||
    stt.failCount >= STT_MAX_FAILS_BEFORE_FALLBACK;
  useEffect(() => {
    if (shouldFallback) setInputMode("KEYBOARD");
  }, [shouldFallback]);

  // 마이크 자동 시작 — 답변 가능 화면에서 음성 모드가 IDLE일 때만
  useEffect(() => {
    if (!canAnswer || inputMode !== "VOICE" || stt.status !== "IDLE") return;
    const timer = setTimeout(() => sttStart(), MIC_START_DELAY_MS);
    return () => clearTimeout(timer);
  }, [canAnswer, inputMode, stt.status, sttStart]);

  // 완료 연출(마지막 피드백 + 리허설 완료) 여운 후 세션 층에 신호
  useEffect(() => {
    if (flow.status !== "COMPLETED") return;
    const timer = setTimeout(onComplete, SIMULATION_COMPLETE_LINGER_MS);
    return () => clearTimeout(timer);
  }, [flow.status, onComplete]);

  const { status: flowStatus, retry: flowRetry } = flow;
  const sttStatus = stt.status;
  const handleAction = useCallback(
    (event: GestureActionEvent) => {
      if (event.action === "CONFIRM" && flowStatus === "FAILED") {
        flowRetry();
        return;
      }
      if (!canAnswer) return;
      if (event.action === "CONFIRM") {
        if (sttStatus === "CANDIDATE") sttConfirm();
        else if (sttStatus === "ERROR") sttRetry();
      }
      if (event.action === "PREV" && sttStatus === "CANDIDATE") {
        sttCancel();
      }
    },
    [
      flowStatus,
      flowRetry,
      canAnswer,
      sttStatus,
      sttConfirm,
      sttRetry,
      sttCancel,
    ],
  );

  useGestureController({
    engine,
    stream,
    onAction: handleAction,
    // 키보드 입력 화면에선 Enter/화살표를 텍스트 입력에 양보한다.
    enabled: !(inputMode === "KEYBOARD" && canAnswer),
  });

  // --- 화면 파생 ---

  if (flow.status === "STARTING") {
    return (
      <CenterColumn>
        <ScanningEffect />
        <StatusLine text="시뮬레이션을 준비하는 중…" />
      </CenterColumn>
    );
  }

  if (flow.status === "FAILED") {
    return <FailedView reason={flow.failReason} />;
  }

  if (flow.status === "EVALUATING") {
    return (
      <CenterColumn>
        {flow.transcript && (
          <SttPanel text={flow.transcript} label="YOUR ANSWER" />
        )}
        <ScanningEffect />
        <StatusLine text="상대가 당신의 말을 듣고 있어요…" />
      </CenterColumn>
    );
  }

  if (flow.status === "NEXT_LINE") {
    return (
      <CenterColumn>
        {flow.evaluation && <FeedbackPanel evaluation={flow.evaluation} />}
        <ScanningEffect />
        <StatusLine text="상대가 다음 말을 고르는 중…" />
      </CenterColumn>
    );
  }

  if (flow.status === "COMPLETED") {
    return (
      <CenterColumn>
        <motion.div
          className="text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]"
          initial={{ opacity: 0, scale: 0.96 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5, ease: "easeOut" }}
        >
          <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/65">
            REHEARSAL COMPLETE
          </p>
          <h2 className="text-4xl font-extralight tracking-wide md:text-5xl">
            리허설 완료
          </h2>
        </motion.div>
        {flow.evaluation && <FeedbackPanel evaluation={flow.evaluation} />}
      </CenterColumn>
    );
  }

  // ANSWERING — 상대 발화 표시 + 답변 대기 (직전 판정이 실패면 재시도 화면)
  return (
    <div className="flex h-full flex-col items-center justify-center gap-10 px-8">
      <OpponentLine
        turn={flow.currentTurn}
        maxTurn={flow.maxTurn}
        sceneCue={flow.sceneCue ?? ""}
        line={flow.opponentLine ?? ""}
        actionPrompt={flow.actionPrompt ?? ""}
      />

      {flow.evaluation && flow.evaluation.outcome === "RETRY_REQUIRED" && (
        <FeedbackPanel evaluation={flow.evaluation} />
      )}

      <AnswerArea
        inputMode={inputMode}
        stt={stt}
        onSubmitTyped={handleAnswer}
        sttLabel="YOUR ANSWER"
        typedPlaceholder="상대에게 할 말을 입력해 주세요"
        autoConfirmMs={SIMULATION_AUTO_CONFIRM_MS}
      />
    </div>
  );
}

/** 상대 발화 — 턴 진행 표시와 함께 지금 응답해야 할 말을 보여준다. */
function OpponentLine({
  turn,
  maxTurn,
  sceneCue,
  line,
  actionPrompt,
}: {
  turn: number;
  maxTurn: number;
  sceneCue: string;
  line: string;
  actionPrompt: string;
}) {
  return (
    <div className="max-w-4xl text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]">
      <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/65">
        SIMULATION · TURN {turn} / {maxTurn}
      </p>
      <p className="mb-3 text-base font-light text-white/65">{sceneCue}</p>
      {/* 발화가 바뀔 때만 다시 페이드 — 같은 턴 재시도에선 출렁이지 않는다 */}
      <motion.h2
        key={`${turn}-${line}`}
        className="text-3xl font-extralight tracking-wide md:text-4xl"
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: "easeOut" }}
      >
        “{line}”
      </motion.h2>
      <p className="mt-5 text-lg font-light text-white/75">{actionPrompt}</p>
    </div>
  );
}

/** 판정 피드백 — 성공/실패 톤만 나누고 문구는 백엔드 그대로 보여준다. */
function FeedbackPanel({ evaluation }: { evaluation: SimulationFeedback }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: "easeOut" }}
    >
      <GlassPanel
        className={`max-w-3xl px-8 py-5 ${
          evaluation.outcome === "ACCEPTED" || evaluation.turnCompleted
            ? "border-white/45"
            : "border-white/15"
        }`}
      >
        <div className="flex flex-col items-center gap-2 text-center">
          <span className="text-xs font-light tracking-[0.3em] text-white/55">
            {evaluation.outcome === "ACCEPTED"
              ? "GOOD"
              : evaluation.outcome === "RETRY_REQUIRED"
                ? "TRY AGAIN"
                : "NEXT TURN"}
          </span>
          {evaluation.feedback && (
            <p className="text-xl font-extralight tracking-wide text-white/90">
              {evaluation.feedback}
            </p>
          )}
          {!evaluation.turnCompleted && (
            <p className="text-sm font-light text-white/60">
              같은 질문에 다시 답해 볼까요 — 준비되면 이야기해 주세요
            </p>
          )}
        </div>
      </GlassPanel>
    </motion.div>
  );
}

function FailedView({ reason }: { reason: SimulationFlowFailReason | null }) {
  // 원인별 문구 — 관람객 안내는 동일하고 운영자가 원인을 가늠할 수 있는 수준만 나눈다
  const text =
    reason === "TIMEOUT"
      ? "처리가 오래 걸리고 있습니다 — 손바닥을 펴거나 Enter로 다시 시도"
      : "진행하지 못했습니다 — 손바닥을 펴거나 Enter로 다시 시도";

  return (
    <CenterColumn>
      <StatusLine text={text} error />
    </CenterColumn>
  );
}

function CenterColumn({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-6 px-8">
      {children}
    </div>
  );
}
