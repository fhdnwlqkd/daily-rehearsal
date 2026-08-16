"use client";

import { useCallback, useEffect, useState } from "react";
import { motion } from "framer-motion";
import { AnswerArea } from "../shared/answer-area";
import type { AnswerInputMode } from "../shared/answer-area";
import { GlassPanel } from "../shared/glass-panel";
import { ScanningEffect } from "../shared/scanning-effect";
import { StatusLine } from "../shared/status-line";
import { useBriefingFlow } from "../../hooks/use-briefing-flow";
import { useGetBriefingContent } from "../../hooks/use-get-briefing-content";
import { useGestureController } from "../../hooks/use-gesture-controller";
import { useSpeechToText } from "../../hooks/use-speech-to-text";
import {
  BRIEFING_AUTO_CONFIRM_MS,
  BRIEFING_COMPLETE_LINGER_MS,
} from "../../lib/briefing/constants";
import { splitSentences } from "../../lib/briefing/sentences";
import { STT_MAX_FAILS_BEFORE_FALLBACK } from "../../lib/stt/constants";
import type {
  BriefingFlowFailReason,
  ContextStatus,
  GestureActionEvent,
  GestureEngineHandle,
  SituationType,
} from "../../types";

/** 화면 진입/재발화 후 마이크를 다시 열기까지의 짧은 텀 (전환 연출과 겹침 완화) */
const MIC_START_DELAY_MS = 600;

interface BriefingStageProps {
  /** 세션 층이 소유한 세션 ID — 스테이지는 소비만 한다. */
  sessionId: string;
  /** 브리핑 내용 조회 경로 키 + 문구용 label. */
  situationType: SituationType;
  /** 부스 수명 장비 — 확정(CONFIRM)/다시 말하기(PREV) 입력용. */
  engine: GestureEngineHandle;
  stream: MediaStream | null;
  /** context 수집 완료(COMPLETED) 연출 후 호출 — 세션 층이 다음 스테이지로 넘긴다. */
  onComplete: () => void;
}

/**
 * 2. 브리핑 — 최초 질문(브리핑) + 재질문(follow-up) 라운드 전체를 담당한다.
 * 재질문은 별도 스테이지가 아니라 이 스테이지의 내부 상태다(라운드는
 * useBriefingFlow의 round 카운터). 흐름 상태머신은 BriefingFlowController에
 * 있고, 여기는 (contentStatus, flowStatus, sttSnapshot, inputMode)에서 화면을
 * 파생하는 조합만 한다.
 *
 * 답변 입력은 음성(STT 자동 확정: 후보 후 카운트다운 → 전송, CONFIRM 즉시
 * 전송, PREV 다시 말하기)이 기본이고, STT 비복구 에러·연속 실패 시 키보드로
 * 전환한다. 두 경로 모두 handleAnswer 하나로 합류해 flow.submitAnswer로 나간다.
 */
export function BriefingStage({
  sessionId,
  situationType,
  engine,
  stream,
  onComplete,
}: BriefingStageProps) {
  const { content, status: contentStatus } = useGetBriefingContent(
    situationType.situationType,
  );
  const flow = useBriefingFlow(sessionId);
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
  // 자동 확정(CANDIDATE 카운트다운 → 전송)은 AnswerArea가 소유한다.

  // 답변을 받을 수 있는 화면인가 — 최초 질문(IDLE) 또는 재질문(FOLLOW_UP)
  const canAnswer =
    (flow.status === "IDLE" && contentStatus === "READY") ||
    flow.status === "FOLLOW_UP";

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

  // 완료 연출 여운 후 세션 층에 신호
  useEffect(() => {
    if (flow.status !== "COMPLETED") return;
    const timer = setTimeout(onComplete, BRIEFING_COMPLETE_LINGER_MS);
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
    // (FAILED 등 입력창이 없는 화면에선 키보드 모드여도 CONFIRM이 필요)
    enabled: !(inputMode === "KEYBOARD" && canAnswer),
  });

  // --- 화면 파생 ---

  if (contentStatus === "LOADING" && flow.status === "IDLE") {
    return (
      <CenterColumn>
        <StatusLine text="브리핑을 준비하는 중…" />
      </CenterColumn>
    );
  }
  if (contentStatus === "ERROR" && flow.status === "IDLE") {
    return (
      <CenterColumn>
        <StatusLine text="브리핑을 불러오지 못했습니다" error />
      </CenterColumn>
    );
  }

  if (flow.status === "SUBMITTING" || flow.status === "PROCESSING") {
    return <ProcessingView serverStatus={flow.serverStatus} />;
  }

  if (flow.status === "COMPLETED") {
    return <CompletedView />;
  }

  if (flow.status === "FAILED") {
    return <FailedView reason={flow.failReason} />;
  }

  // IDLE(최초 질문) 또는 FOLLOW_UP(재질문)
  return (
    <div className="flex h-full flex-col items-center justify-center gap-10 px-8">
      {flow.status === "FOLLOW_UP" ? (
        <FollowUpQuestions questions={flow.followUpQuestions} />
      ) : (
        <BriefingQuestion
          title={content?.briefingTitle ?? ""}
          example={content?.exampleAnswer ?? ""}
        />
      )}

      <AnswerArea
        inputMode={inputMode}
        stt={stt}
        onSubmitTyped={handleAnswer}
        sttLabel="YOUR BRIEFING"
        typedPlaceholder="내일의 상황을 입력해 주세요"
        autoConfirmMs={BRIEFING_AUTO_CONFIRM_MS}
      />
    </div>
  );
}

/** 최초 질문 — 타입별 고정 브리핑 질문과 예시 답변(발화 유도)을 함께 보여준다. */
function BriefingQuestion({
  title,
  example,
}: {
  title: string;
  example: string;
}) {
  const titleSentences = splitSentences(title);
  const exampleSentences = splitSentences(example);

  return (
    <div className="w-full max-w-4xl drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]">
      <p className="mb-4 text-center text-xs font-light tracking-[0.34em] text-white/65">
        BRIEFING
      </p>
      <GlassPanel className="w-full px-8 py-6 md:px-10">
        <div className="space-y-3 text-left">
          {titleSentences.map((sentence, index) => (
            <p
              key={`${sentence}-${index}`}
              className={`text-xl leading-[1.5] tracking-[0.01em] break-keep md:text-2xl ${
                index === 0
                  ? "font-medium text-white"
                  : "font-light text-white/[0.88]"
              }`}
            >
              {sentence}
            </p>
          ))}
        </div>

        {/* 예시는 질문과 분리해 사용자가 그대로 읽어야 하는 답으로 오해하지 않게 한다. */}
        <div className="mt-5 border-t border-white/15 pt-4">
          <p className="text-xs font-medium tracking-[0.18em] text-white/55">
            이렇게 말해볼 수 있어요
          </p>
          <div className="mt-2 space-y-1.5">
            {exampleSentences.map((sentence, index) => (
              <p
                key={`${sentence}-${index}`}
                className="text-lg leading-[1.5] font-light break-keep text-white/[0.82] md:text-xl"
              >
                {index === 0 ? "“" : ""}
                {sentence}
                {index === exampleSentences.length - 1 ? "”" : ""}
              </p>
            ))}
          </div>
        </div>
      </GlassPanel>
    </div>
  );
}

/** 재질문 — 백엔드가 내려준 질문 리스트를 한 번에 보여주고 답변 한 번으로 받는다. */
function FollowUpQuestions({ questions }: { questions: string[] }) {
  return (
    <div className="flex max-w-3xl flex-col items-center gap-8">
      <div className="text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]">
        <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/65">
          FOLLOW-UP
        </p>
        <h2 className="text-3xl font-extralight tracking-wide md:text-4xl">
          몇 가지만 더 알려주세요
        </h2>
      </div>
      <GlassPanel className="w-full px-8 py-6">
        <ol className="flex flex-col gap-4">
          {questions.map((question, index) => (
            <motion.li
              key={question}
              className="flex items-baseline gap-4"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.12, duration: 0.3 }}
            >
              <span className="text-xs font-light tracking-[0.3em] text-white/45">
                {String(index + 1).padStart(2, "0")}
              </span>
              <span className="text-2xl font-extralight tracking-wide text-white/90">
                {question}
              </span>
            </motion.li>
          ))}
        </ol>
      </GlassPanel>
    </div>
  );
}

function ProcessingView({
  serverStatus,
}: {
  serverStatus: ContextStatus | null;
}) {
  const text =
    serverStatus === "MERGING"
      ? "답변을 반영하는 중…"
      : "내일의 상황을 정리하는 중…";

  return (
    <div className="flex h-full flex-col items-center justify-center gap-6">
      <ScanningEffect />
      <StatusLine text={text} />
    </div>
  );
}

function CompletedView() {
  return (
    <CenterColumn>
      <motion.div
        className="text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]"
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
      >
        <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/65">
          CONTEXT READY
        </p>
        <h2 className="text-4xl font-extralight tracking-wide md:text-5xl">
          상황 준비 완료
        </h2>
      </motion.div>
    </CenterColumn>
  );
}

function FailedView({ reason }: { reason: BriefingFlowFailReason | null }) {
  // 원인별 문구 — 관람객 안내는 동일하고 운영자가 원인을 가늠할 수 있는 수준만 나눈다
  const text =
    reason === "TIMEOUT"
      ? "처리가 오래 걸리고 있습니다 — 손바닥을 펴거나 Enter로 다시 시도"
      : "답변을 처리하지 못했습니다 — 손바닥을 펴거나 Enter로 다시 시도";

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
