"use client";

import { motion } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import { useTicketFlow } from "../../hooks/use-ticket-flow";
import type {
  SessionRecorderStatus,
  SessionRecording,
} from "../../hooks/use-session-recorder";

interface TicketStageProps {
  sessionId: string;
  recorderStatus: SessionRecorderStatus;
  recording: SessionRecording | null;
}

export function TicketStage({
  sessionId,
  recorderStatus,
  recording,
}: TicketStageProps) {
  const flow = useTicketFlow(sessionId, recorderStatus, recording);

  if (
    flow.status !== "COMPLETED" ||
    !flow.ticket?.snapshot ||
    !flow.ticket.changeCard
  ) {
    return (
      <TicketProgress status={flow.status} errorMessage={flow.errorMessage} />
    );
  }

  const { snapshot, changeCard, qrPayload } = flow.ticket;
  return (
    <motion.section
      className="mx-auto grid h-[min(78vh,760px)] w-[min(92vw,1280px)] overflow-hidden rounded-lg border border-white/30 bg-white text-neutral-950 shadow-2xl lg:grid-cols-[1.65fr_1fr]"
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, ease: "easeOut" }}
    >
      <div className="flex min-h-0 flex-col px-7 py-8 sm:px-12 sm:py-11">
        <p className="text-xs font-medium tracking-[0.32em] text-neutral-500">
          CHANGE CARD
        </p>
        <h2 className="mt-5 text-3xl font-light sm:text-5xl">
          내일의 변화 카드
        </h2>

        <dl className="mt-9 grid flex-1 content-start gap-0 border-t border-neutral-300">
          <TicketFact label="상황" value={snapshot.situationLabel} />
          <TicketFact
            label="내일의 중요한 순간"
            value={snapshot.criticalMoment}
          />
          <TicketFact label="목표 인상" value={snapshot.desiredPersonaLabel} />
          <TicketFact
            label="선택한 스타일"
            value={snapshot.selectedOutfitLabel}
          />
        </dl>

        <p className="mt-8 text-sm text-neutral-500">
          내일의 리스크를 줄이는 행동 변화가 저장되었습니다.
        </p>
      </div>

      <div className="flex min-h-0 flex-col bg-neutral-950 px-7 py-8 text-white sm:px-10 sm:py-11">
        <div className="space-y-8">
          <ChangePlan label="오늘의 행동 변화" value={changeCard.todayAction} />
          <ChangePlan
            label="내일 유지할 태도"
            value={changeCard.tomorrowAttitude}
          />
          <ChangePlan label="If-Then" value={changeCard.ifThenPlan} />
        </div>

        <div className="mt-auto flex flex-col items-center pt-8 text-center">
          {qrPayload && (
            <div className="bg-white p-3">
              <QRCodeSVG value={qrPayload} size={152} marginSize={0} />
            </div>
          )}
          <p className="mt-5 text-xs font-light tracking-[0.3em] text-white/55">
            SCAN TO SAVE
          </p>
          <p className="mt-3 max-w-xs text-sm leading-6 text-white/50">
            개인 폰에서 오늘의 변화 카드와 영상을 확인하세요.
          </p>
        </div>
      </div>
    </motion.section>
  );
}

function TicketFact({ label, value }: { label: string; value: string }) {
  return (
    <div className="border-b border-neutral-200 py-5 sm:py-6">
      <dt className="text-sm text-neutral-500">{label}</dt>
      <dd className="mt-2 text-xl leading-snug font-light sm:text-3xl">
        {value}
      </dd>
    </div>
  );
}

function ChangePlan({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-sm text-white/55">{label}</p>
      <p className="mt-2 text-xl leading-relaxed font-light text-white sm:text-2xl">
        {value}
      </p>
    </div>
  );
}

function TicketProgress({
  status,
  errorMessage,
}: {
  status: ReturnType<typeof useTicketFlow>["status"];
  errorMessage: string | null;
}) {
  const message =
    errorMessage ??
    (status === "UPLOADING_VIDEO"
      ? "영상을 저장하고 있습니다"
      : status === "WAITING_FOR_VIDEO"
        ? "영상 업로드를 확인하고 있습니다"
        : status === "GENERATING"
          ? "변화 카드를 만들고 있습니다"
          : "기록을 정리하고 있습니다");

  return (
    <div className="flex h-full flex-col items-center justify-center gap-5 px-8 text-center">
      {status !== "FAILED" && (
        <motion.div
          className="h-10 w-10 rounded-full border-2 border-white/20 border-t-white"
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
        />
      )}
      <p className="text-3xl font-light text-white sm:text-4xl">{message}</p>
    </div>
  );
}
