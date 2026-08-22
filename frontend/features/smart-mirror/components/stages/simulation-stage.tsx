"use client";

import { useCallback, useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { AnswerArea } from "../shared/answer-area";
import type { AnswerInputMode } from "../shared/answer-area";
import { GlassPanel } from "../shared/glass-panel";
import { ScanningEffect } from "../shared/scanning-effect";
import { StatusLine } from "../shared/status-line";
import { StageCountdown } from "../shared/stage-countdown";
import { SttPanel } from "../shared/stt-panel";
import { useCountdown } from "../../hooks/use-countdown";
import { useGestureController } from "../../hooks/use-gesture-controller";
import { useSimulationFlow } from "../../hooks/use-simulation-flow";
import { useSpeechToText } from "../../hooks/use-speech-to-text";
import {
  SIMULATION_AUTO_CONFIRM_MS,
  SIMULATION_COMPLETE_LINGER_MS,
  SIMULATION_INTRO_DURATION_MS,
} from "../../lib/simulation/constants";
import { STT_MAX_FAILS_BEFORE_FALLBACK } from "../../lib/stt/constants";
import { SIMULATION_DURATION_SECONDS } from "../../lib/timing/constants";
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
  /** Decart 트랙을 끊기 전에 녹화의 마지막 청크를 확정한다. */
  onStopRecording: () => void;
  /** 정상 완료 또는 제한시간 만료 시 Decart 연결을 즉시 끊는다. */
  onStopDecart: () => void;
  /** 전체 턴 성공(COMPLETED) 연출 후 호출 — 세션 층이 티켓으로 넘긴다. */
  onComplete: () => void;
}

/**
 * 4. 시뮬레이션 — 상황 속 대화를 N턴 연습한다 (이슈 #68).
 * 턴 반복은 이 스테이지의 내부 상태다(브리핑의 재질문과 같은 원칙):
 * 턴마다 발화→판정/피드백을 받고, 실패하면 같은 턴을 한 번 재시도한다.
 * maxTurn 도달 또는 전체 제한시간 만료 판정은 프론트 책임이다.
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
  onStopRecording,
  onStopDecart,
  onComplete,
}: SimulationStageProps) {
  const flow = useSimulationFlow(sessionId);
  const [inputMode, setInputMode] = useState<AnswerInputMode>("VOICE");
  const [timeLimitReached, setTimeLimitReached] = useState(false);

  // 턴 인트로(1·2번: 턴 표시 + 상황) → 본문(3·4번: 상대 발화 + 행동 요구) 연출.
  // 턴이 실제로 바뀔 때만(재시도 제외) 인트로를 다시 재생한다.
  const [revealPhase, setRevealPhase] = useState<"INTRO" | "MAIN">("INTRO");
  const turnKey = `${flow.currentTurn}-${flow.opponentLine ?? ""}`;
  useEffect(() => {
    setRevealPhase("INTRO");
    const timer = setTimeout(
      () => setRevealPhase("MAIN"),
      SIMULATION_INTRO_DURATION_MS,
    );
    return () => clearTimeout(timer);
  }, [turnKey]);

  const { finish } = flow;
  const handleTimeLimitReached = useCallback(() => {
    setTimeLimitReached(true);
    onStopRecording();
    onStopDecart();
    finish();
  }, [finish, onStopRecording, onStopDecart]);
  const remainingSeconds = useCountdown({
    durationSeconds: SIMULATION_DURATION_SECONDS,
    onExpire: handleTimeLimitReached,
  });

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

  // 마이크 자동 시작 — 답변 가능 화면에서, 인트로가 끝나 본문(상대 발화)이
  // 보이고, 음성 모드가 IDLE일 때만. 인트로 중엔 상대 발화를 아직 안 보여줬으니
  // 마이크부터 열리지 않게 막는다.
  useEffect(() => {
    if (
      !canAnswer ||
      revealPhase !== "MAIN" ||
      inputMode !== "VOICE" ||
      stt.status !== "IDLE"
    )
      return;
    const timer = setTimeout(() => sttStart(), MIC_START_DELAY_MS);
    return () => clearTimeout(timer);
  }, [canAnswer, revealPhase, inputMode, stt.status, sttStart]);

  // 완료 연출(마지막 피드백 + 리허설 완료) 여운 후 세션 층에 신호
  useEffect(() => {
    if (flow.status !== "COMPLETED") return;
    onStopRecording();
    onStopDecart();
    const lingerMs = timeLimitReached ? 0 : SIMULATION_COMPLETE_LINGER_MS;
    const timer = setTimeout(onComplete, lingerMs);
    return () => clearTimeout(timer);
  }, [
    flow.status,
    timeLimitReached,
    onStopRecording,
    onStopDecart,
    onComplete,
  ]);

  const { status: flowStatus, retry: flowRetry } = flow;
  const sttStatus = stt.status;
  const handleAction = useCallback(
    (event: GestureActionEvent) => {
      if (event.action === "CONFIRM" && flowStatus === "FAILED") {
        flowRetry();
        return;
      }
      if (!canAnswer || revealPhase !== "MAIN") return;
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
      revealPhase,
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

  const countdown =
    flow.status === "COMPLETED" ? null : (
      <StageCountdown
        label="리허설 종료까지"
        remainingSeconds={remainingSeconds}
      />
    );

  if (flow.status === "STARTING") {
    return (
      <>
        {countdown}
        <CenterColumn>
          <ScanningEffect />
          <StatusLine text="시뮬레이션을 준비하는 중…" />
        </CenterColumn>
      </>
    );
  }

  if (flow.status === "FAILED") {
    return (
      <>
        {countdown}
        <FailedView reason={flow.failReason} />
      </>
    );
  }

  if (flow.status === "FINISHING") {
    return (
      <>
        {countdown}
        <CenterColumn>
          <ScanningEffect />
          <StatusLine text="제한시간이 끝나 리허설을 마무리하고 있어요…" />
        </CenterColumn>
      </>
    );
  }

  if (flow.status === "EVALUATING") {
    return (
      <>
        {countdown}
        <CenterColumn>
          {flow.transcript && (
            <SttPanel text={flow.transcript} label="YOUR ANSWER" />
          )}
          <ScanningEffect />
          <StatusLine text="상대가 당신의 말을 듣고 있어요…" />
        </CenterColumn>
      </>
    );
  }

  if (flow.status === "NEXT_LINE") {
    return (
      <>
        {countdown}
        <CenterColumn>
          {flow.evaluation && <FeedbackPanel evaluation={flow.evaluation} />}
          <ScanningEffect />
          <StatusLine text="상대가 다음 말을 고르는 중…" />
        </CenterColumn>
      </>
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
          <p className="mb-[clamp(0.5rem,1.5vh,1rem)] text-xs font-light tracking-[0.34em] text-white/65">
            REHEARSAL COMPLETE
          </p>
          <h2 className="text-[clamp(1.5rem,4.5vw,3rem)] font-extralight tracking-wide">
            리허설 완료
          </h2>
        </motion.div>
        {flow.evaluation && <FeedbackPanel evaluation={flow.evaluation} />}
      </CenterColumn>
    );
  }

  // ANSWERING — 인트로(턴 표시+상황) → 본문(상대 발화+답변 대기) 순서로 보여준다
  // (직전 판정이 실패면 본문은 재시도 화면이 된다)
  return (
    <>
      {countdown}
      {/* CenterColumn과 같은 세이프존+스크롤 구조 — 긴 피드백/재시도 화면 대응(#232) */}
      <div className="h-full overflow-y-auto px-[clamp(1rem,4vw,2rem)] pt-[clamp(5.5rem,16vh,7.5rem)] pb-[clamp(3.25rem,10vh,4.5rem)]">
        <div className="flex min-h-full w-full flex-col items-center justify-center gap-[clamp(1rem,3.5vh,2.5rem)]">
          <AnimatePresence mode="wait">
            {revealPhase === "INTRO" ? (
              <IntroCue
                key={`intro-${turnKey}`}
                turn={flow.currentTurn}
                maxTurn={flow.maxTurn}
                sceneCue={flow.sceneCue ?? ""}
              />
            ) : (
              <motion.div
                key={`main-${turnKey}`}
                className="flex flex-col items-center gap-[clamp(1rem,3.5vh,2.5rem)]"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.45, ease: "easeOut" }}
              >
                <OpponentLine
                  turn={flow.currentTurn}
                  line={flow.opponentLine ?? ""}
                  actionPrompt={flow.actionPrompt ?? ""}
                />

                {flow.evaluation &&
                  flow.evaluation.outcome === "RETRY_REQUIRED" && (
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
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </>
  );
}

/** 인트로 연출(1·2번) — 지금 몇 턴째이고 어떤 상황인지 큰 글씨로 먼저 알려준다. */
function IntroCue({
  turn,
  maxTurn,
  sceneCue,
}: {
  turn: number;
  maxTurn: number;
  sceneCue: string;
}) {
  return (
    <motion.div
      className="max-w-4xl text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]"
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.45, ease: "easeOut" }}
    >
      <p className="mb-[clamp(0.625rem,1.8vh,1.25rem)] text-sm font-normal tracking-[0.34em] text-white/90">
        SIMULATION · TURN {turn} / {maxTurn}
      </p>
      <p className="text-[clamp(1.125rem,2.6vw,1.875rem)] font-light break-keep text-white/85">
        {sceneCue}
      </p>
    </motion.div>
  );
}

/** 본문 연출(3·4번) — 지금 응답해야 할 상대 발화와 행동 요구를 보여준다. */
function OpponentLine({
  turn,
  line,
  actionPrompt,
}: {
  turn: number;
  line: string;
  actionPrompt: string;
}) {
  return (
    <div className="max-w-4xl text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]">
      {/* 발화가 바뀔 때만 다시 페이드 — 같은 턴 재시도에선 출렁이지 않는다 */}
      <motion.h2
        key={`${turn}-${line}`}
        className="text-[clamp(1.25rem,3.2vw,2.5rem)] font-extralight tracking-wide break-keep"
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: "easeOut" }}
      >
        “{line}”
      </motion.h2>
      <p className="mt-[clamp(0.625rem,1.8vh,1.25rem)] text-[clamp(0.9375rem,2vw,1.125rem)] font-light break-keep text-white/75">
        {actionPrompt}
      </p>
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
        className={`max-w-3xl px-[clamp(1.125rem,2.5vw,2rem)] py-[clamp(0.875rem,2vh,1.25rem)] ${
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
            <p className="text-[clamp(1rem,2vw,1.25rem)] leading-[1.55] font-extralight tracking-wide break-keep text-white/90">
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
    // 상단 pt = 헤더+카운트다운 세이프존(#232) — 긴 피드백이 중앙정렬로
    // 위로 확장돼 헤더·REC·타이머를 덮던 문제를 막는다. 그래도 넘치면
    // 내부 스크롤로 살린다. justify-center는 공간이 남을 때만 작동하므로
    // 짧은 콘텐츠는 기존처럼 중앙에 온다.
    <div className="h-full overflow-y-auto px-[clamp(1rem,4vw,2rem)] pt-[clamp(5.5rem,16vh,7.5rem)] pb-[clamp(3.25rem,10vh,4.5rem)]">
      <div className="flex min-h-full w-full flex-col items-center justify-center gap-6">
        {children}
      </div>
    </div>
  );
}
