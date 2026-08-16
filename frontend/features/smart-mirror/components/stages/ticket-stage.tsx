"use client";

import { motion } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import { useEffect, useState } from "react";
import { StatusLine } from "../shared/status-line";
import { StageFrame } from "../stage-frame";
import { experiencePhases } from "../../data/phases";
import { useTicketFlow } from "../../hooks/use-ticket-flow";
import {
  ticketPreviewData,
  type TicketPreviewSituation,
} from "../../lib/ticket/preview-data";
import { buildTicketDownloadUrl } from "../../lib/ticket/download-url";
import type { ChangeCard, TicketSnapshot } from "../../types";
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
  const downloadUrl = browserDownloadUrl(sessionId, qrPayload);
  return (
    <TicketCard
      snapshot={snapshot}
      changeCard={changeCard}
      qrPayload={downloadUrl}
    />
  );
}

interface TicketCardProps {
  snapshot: TicketSnapshot;
  changeCard: ChangeCard;
  qrPayload?: string;
}

function TicketCard({ snapshot, changeCard, qrPayload }: TicketCardProps) {
  const density = getTicketDensity(snapshot, changeCard);

  return (
    <motion.section
      className="absolute inset-0 overflow-hidden bg-[#e9eef1] text-[#172027]"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5, ease: "easeOut" }}
    >
      <div className="h-full px-[clamp(24px,4vw,48px)] pt-[clamp(18px,2.5vh,28px)] pb-[clamp(58px,6vh,72px)] [@media(max-height:800px)]:pt-3 [@media(max-height:800px)]:pb-14">
        <div className="mx-auto flex h-full min-h-0 w-full max-w-[840px] flex-col [@media(max-height:800px)]:max-w-[1040px]">
          <header className="flex shrink-0 items-center justify-between gap-6 pb-[clamp(12px,1.8vh,18px)] [@media(max-height:800px)]:pb-2">
            <div>
              <p className="flex items-center gap-2.5 text-base font-semibold tracking-[0.14em] text-[#52616b] uppercase">
                <span className="h-2 w-2 rounded-full bg-[#00B0F0]" />
                Rehearsal complete
              </p>
              <h2 className="mt-1.5 text-[clamp(38px,4.8vw,52px)] leading-[1.1] font-semibold tracking-[-0.025em] text-[#121a20] [@media(max-height:800px)]:mt-0.5 [@media(max-height:800px)]:text-[38px]">
                내일을 위한 티켓
              </h2>
            </div>
            <p className="text-right text-base leading-[1.45] text-[#66757f]">
              오늘의 연습을
              <br />한 장에 담았습니다
            </p>
          </header>

          <article className="relative flex shrink-0 flex-col overflow-hidden rounded-[24px] border border-[#dce3e7] bg-white shadow-[0_16px_44px_rgba(24,39,49,0.09)]">
            <div className="grid shrink-0 grid-cols-[minmax(0,1fr)_auto] items-center gap-5 px-[clamp(24px,4vw,38px)] py-[clamp(12px,1.8vh,18px)] [@media(max-height:800px)]:px-8 [@media(max-height:800px)]:py-2.5">
              <div className="min-w-0">
                <p className="text-sm font-semibold tracking-[0.16em] text-[#73808a] uppercase">
                  Daily Rehearsal · Result Ticket
                </p>
                <p className="mt-3 text-base font-semibold text-[#00B0F0] [@media(max-height:800px)]:mt-1.5">
                  {snapshot.situationLabel}
                </p>
                <h3 className="mt-1 text-[clamp(34px,4.25vw,46px)] leading-[1.12] font-semibold tracking-[-0.025em] text-[#172027] [@media(max-height:800px)]:text-[34px]">
                  내일 기억할 세 가지
                </h3>
                <p className="mt-2.5 max-w-lg text-[17px] leading-[1.5] text-[#6a7881] [@media(max-height:800px)]:mt-1">
                  오늘의 리허설에서 찾은 행동을 내일의 장면에 가져가세요.
                </p>
              </div>

              {qrPayload && (
                <div className="shrink-0 border border-[#dce3e7] bg-white p-2.5">
                  <QRCodeSVG
                    value={qrPayload}
                    size={96}
                    marginSize={0}
                    bgColor="#ffffff"
                    fgColor="#172027"
                    className="h-[clamp(78px,11vw,96px)] w-[clamp(78px,11vw,96px)]"
                  />
                </div>
              )}
            </div>

            <TicketPerforation />

            <div className="flex flex-col px-[clamp(24px,4vw,38px)] pt-[clamp(14px,2vh,20px)] pb-[clamp(14px,2vh,20px)] [@media(max-height:800px)]:grid [@media(max-height:800px)]:grid-cols-[0.85fr_1.5fr] [@media(max-height:800px)]:px-8 [@media(max-height:800px)]:pt-2 [@media(max-height:800px)]:pb-3">
              <dl className="grid shrink-0 grid-cols-2 gap-x-8 gap-y-3.5 border-b border-[#e5eaed] pb-[clamp(14px,1.8vh,18px)] [@media(max-height:800px)]:gap-y-2.5 [@media(max-height:800px)]:border-r [@media(max-height:800px)]:border-b-0 [@media(max-height:800px)]:pr-7 [@media(max-height:800px)]:pb-0">
                <TicketFact
                  label="중요한 순간"
                  value={snapshot.criticalMoment}
                  density={density}
                />
                <TicketFact
                  label="목표 인상"
                  value={snapshot.desiredPersonaLabel}
                  density={density}
                />
              </dl>

              <ol className="mt-1 flex flex-col divide-y divide-[#e7ecef] [@media(max-height:800px)]:mt-0 [@media(max-height:800px)]:pl-7">
                <ChangePlan
                  number="01"
                  label="먼저 바꿀 행동"
                  value={changeCard.todayAction}
                  density={density}
                />
                <ChangePlan
                  number="02"
                  label="유지할 태도"
                  value={changeCard.tomorrowAttitude}
                  density={density}
                />
                <ChangePlan
                  number="03"
                  label="막히는 순간에는"
                  value={changeCard.ifThenPlan}
                  density={density}
                />
              </ol>

              <footer className="flex shrink-0 items-center justify-between gap-5 border-t border-[#e5eaed] pt-[clamp(12px,1.6vh,16px)] [@media(max-height:800px)]:col-span-2 [@media(max-height:800px)]:mt-2 [@media(max-height:800px)]:pt-2">
                <div>
                  <p className="text-base font-semibold text-[#26343d]">
                    결과와 영상 가져가기
                  </p>
                  <p className="mt-1 text-sm leading-relaxed text-[#75828b]">
                    상단 QR을 휴대폰으로 스캔해주세요
                  </p>
                </div>
                <span className="rounded-full border border-[#cfd9de] px-3.5 py-2 text-sm font-semibold tracking-[0.12em] text-[#53636d] uppercase">
                  Valid today
                </span>
              </footer>
            </div>
          </article>
        </div>
      </div>
    </motion.section>
  );
}

/** 백엔드·카메라 없이 실제 티켓 레이아웃을 확인하는 개발 전용 미리보기. */
export function TicketPreview({
  situation = "date",
}: {
  situation?: TicketPreviewSituation;
}) {
  const { snapshot, changeCard } = ticketPreviewData[situation];
  const [qrPayload, setQrPayload] = useState<string>();

  useEffect(() => {
    const url = new URL("/dev/ticket-download-preview", window.location.origin);
    url.searchParams.set("situation", situation);
    setQrPayload(url.toString());
  }, [situation]);

  const phaseIndex = experiencePhases.findIndex(
    (phase) => phase.id === "ticket",
  );
  const ticketPhase = experiencePhases[phaseIndex];
  if (!ticketPhase) return null;

  return (
    <main className="relative h-dvh w-screen overflow-hidden bg-[#e9eef1]">
      <StageFrame
        phase={ticketPhase}
        phaseIndex={phaseIndex}
        totalPhases={experiencePhases.length}
      >
        <TicketCard
          snapshot={snapshot}
          changeCard={changeCard}
          qrPayload={qrPayload}
        />
      </StageFrame>
    </main>
  );
}

function browserDownloadUrl(
  sessionId: string,
  fallbackUrl?: string,
): string | undefined {
  if (typeof window !== "undefined") {
    return buildTicketDownloadUrl(window.location.origin, sessionId);
  }

  return fallbackUrl;
}

function TicketFact({
  label,
  value,
  density,
}: {
  label: string;
  value: string;
  density: TicketDensity;
}) {
  const valueSize =
    density === "tight" || value.length > 44 ? "text-lg" : "text-xl";

  return (
    <div className="min-w-0">
      <dt className="text-base font-semibold tracking-[0.06em] text-[#6f7e88] uppercase">
        {label}
      </dt>
      <dd
        className={`mt-1.5 leading-[1.4] font-medium break-keep text-[#26343d] ${valueSize}`}
      >
        {value}
      </dd>
    </div>
  );
}

function ChangePlan({
  number,
  label,
  value,
  density,
}: {
  number: string;
  label: string;
  value: string;
  density: TicketDensity;
}) {
  const valueSize =
    density === "tight" || value.length > 55
      ? "text-lg"
      : density === "compact"
        ? "text-[19px]"
        : "text-xl";

  return (
    <li className="grid grid-cols-[2.75rem_minmax(0,1fr)] items-start gap-x-3 py-[clamp(13px,1.8vh,18px)] [@media(max-height:800px)]:py-2.5">
      <span className="pt-0.5 text-sm font-semibold text-[#00B0F0]">
        {number}
      </span>
      <div className="min-w-0">
        <p className="text-base font-semibold tracking-[0.04em] text-[#6f7e88] uppercase">
          {label}
        </p>
        <p
          className={`mt-1.5 leading-relaxed font-medium break-keep text-[#26343d] ${valueSize}`}
        >
          {value}
        </p>
      </div>
    </li>
  );
}

type TicketDensity = "comfortable" | "compact" | "tight";

function TicketPerforation() {
  return (
    <div className="relative flex h-7 shrink-0 items-center" aria-hidden>
      <span className="absolute left-0 h-7 w-3.5 -translate-x-1/2 rounded-r-full bg-[#e9eef1]" />
      <div className="mx-5 w-full border-t border-dashed border-[#00B0F0]/65" />
      <span className="absolute right-0 h-7 w-3.5 translate-x-1/2 rounded-l-full bg-[#e9eef1]" />
    </div>
  );
}

function getTicketDensity(
  snapshot: TicketSnapshot,
  changeCard: ChangeCard,
): TicketDensity {
  const totalLength = [
    snapshot.criticalMoment,
    snapshot.desiredPersonaLabel,
    changeCard.todayAction,
    changeCard.tomorrowAttitude,
    changeCard.ifThenPlan,
  ].reduce((sum, value) => sum + value.length, 0);

  if (totalLength > 300) return "tight";
  if (totalLength > 220) return "compact";
  return "comfortable";
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
    <div className="absolute inset-0 flex flex-col items-center justify-center gap-5 bg-[#e9eef1] px-8 text-center">
      <div>
        <p className="mb-4 text-xs font-semibold tracking-[0.28em] text-[#00B0F0]">
          CHANGE CARD
        </p>
        <h2 className="text-3xl font-semibold tracking-[-0.03em] text-[#172027] md:text-4xl">
          티켓을 준비하고 있습니다
        </h2>
      </div>
      <StatusLine text={message} error={status === "FAILED"} />
    </div>
  );
}
