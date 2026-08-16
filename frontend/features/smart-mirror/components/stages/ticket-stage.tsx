"use client";

import { motion } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import { StatusLine } from "../shared/status-line";
import { StageFrame } from "../stage-frame";
import { experiencePhases } from "../../data/phases";
import { useTicketFlow } from "../../hooks/use-ticket-flow";
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
  return (
    <TicketCard
      snapshot={snapshot}
      changeCard={changeCard}
      qrPayload={qrPayload}
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
  const accentColor = getSituationAccent(snapshot.situationLabel);

  return (
    <motion.section
      className="absolute inset-0 overflow-hidden bg-[#252a2e] text-[#f0eadf]"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5, ease: "easeOut" }}
    >
      <div className="h-full px-[clamp(40px,6vw,64px)] pt-[clamp(96px,9vh,124px)] pb-[clamp(88px,8vh,108px)]">
        <div className="mx-auto flex h-full w-full max-w-3xl flex-col">
          <header>
            <p className="text-sm font-light" style={{ color: accentColor }}>
              {snapshot.situationLabel} 리허설 결과
            </p>
            <h2 className="mt-3 max-w-2xl text-4xl leading-[1.18] font-extralight tracking-wide md:text-5xl">
              내일 기억할 세 가지
            </h2>
          </header>

          <dl className="mt-8 border-y border-[#f0eadf]/20">
            <div className="py-4">
              <TicketFact
                label="중요한 순간"
                value={snapshot.criticalMoment}
                density={density}
                inline
              />
            </div>
            <div className="grid grid-cols-2 border-t border-[#f0eadf]/15">
              <div className="py-4 pr-6">
                <TicketFact
                  label="목표 인상"
                  value={snapshot.desiredPersonaLabel}
                  density={density}
                />
              </div>
              <div className="border-l border-[#f0eadf]/15 py-4 pl-6">
                <TicketFact
                  label="선택한 스타일"
                  value={snapshot.selectedOutfitLabel}
                  density={density}
                />
              </div>
            </div>
          </dl>

          <ol className="mt-8 divide-y divide-[#f0eadf]/15 border-y border-[#f0eadf]/20">
            <ChangePlan
              number="01"
              label="먼저 바꿀 행동"
              value={changeCard.todayAction}
              density={density}
              accentColor={accentColor}
            />
            <ChangePlan
              number="02"
              label="유지할 태도"
              value={changeCard.tomorrowAttitude}
              density={density}
              accentColor={accentColor}
            />
            <ChangePlan
              number="03"
              label="막히는 순간에는"
              value={changeCard.ifThenPlan}
              density={density}
              accentColor={accentColor}
            />
          </ol>

          <footer className="mt-auto flex min-h-32 items-end justify-between gap-8 border-t border-[#f0eadf]/20 pt-5">
            <div className="max-w-sm pb-1">
              <p className="text-sm font-light text-[#f0eadf]/70">결과 저장</p>
              <p className="mt-2 text-sm leading-relaxed font-light text-[#f0eadf]/45">
                휴대폰으로 변화 카드와 리허설 영상을 가져갈 수 있습니다.
              </p>
            </div>

            {qrPayload && (
              <div className="shrink-0">
                <div className="bg-[#f0eadf] p-2">
                  <QRCodeSVG
                    value={qrPayload}
                    size={112}
                    marginSize={0}
                    bgColor="#f0eadf"
                    fgColor="#252a2e"
                    className="h-[clamp(104px,14vw,112px)] w-[clamp(104px,14vw,112px)]"
                  />
                </div>
              </div>
            )}
          </footer>
        </div>
      </div>
    </motion.section>
  );
}

type TicketPreviewSituation = "date" | "interview" | "first-day";

const ticketPreviewData: Record<
  TicketPreviewSituation,
  { snapshot: TicketSnapshot; changeCard: ChangeCard }
> = {
  date: {
    snapshot: {
      situationLabel: "소개팅",
      criticalMoment: "첫 인사 뒤 대화가 잠시 끊기는 순간",
      desiredPersonaLabel: "차분하고 자연스러운 인상",
      selectedOutfitLabel: "단정한 네이비 재킷과 밝은 셔츠",
    },
    changeCard: {
      todayAction: "상대의 말을 끝까지 듣고, 답변 속 소재로 질문을 이어가기",
      tomorrowAttitude: "서두르지 않고 편안한 표정과 말하기 속도를 유지하기",
      ifThenPlan: "대화가 끊기면 호흡을 고른 뒤 오늘 가장 기대한 일을 묻기",
    },
  },
  interview: {
    snapshot: {
      situationLabel: "면접",
      criticalMoment: "프로젝트 기여도를 구체적으로 설명해야 하는 순간",
      desiredPersonaLabel: "차분하고 논리적인 인상",
      selectedOutfitLabel: "단정한 차콜 수트와 밝은 셔츠",
    },
    changeCard: {
      todayAction:
        "결론을 먼저 말한 뒤 내가 맡은 행동과 결과를 차례로 설명하기",
      tomorrowAttitude: "질문의 의도를 확인하고 짧게 생각한 뒤 또렷하게 답하기",
      ifThenPlan: "답변이 막히면 상황과 역할부터 나누어 한 문장씩 설명하기",
    },
  },
  "first-day": {
    snapshot: {
      situationLabel: "첫 출근",
      criticalMoment: "팀원들 앞에서 처음 자기소개를 시작하는 순간",
      desiredPersonaLabel: "밝고 신뢰할 수 있는 인상",
      selectedOutfitLabel: "단정한 셔츠와 차분한 슬랙스",
    },
    changeCard: {
      todayAction: "이름과 역할을 먼저 밝히고 함께하고 싶은 일을 짧게 전하기",
      tomorrowAttitude: "모르는 것은 솔직히 묻고 들은 내용은 한 번 확인하기",
      ifThenPlan: "갑작스러운 질문을 받으면 아는 범위를 말하고 확인을 약속하기",
    },
  },
};

/** 백엔드·카메라 없이 실제 티켓 레이아웃을 확인하는 개발 전용 미리보기. */
export function TicketPreview({
  situation = "date",
}: {
  situation?: TicketPreviewSituation;
}) {
  const { snapshot, changeCard } = ticketPreviewData[situation];
  const phaseIndex = experiencePhases.findIndex(
    (phase) => phase.id === "ticket",
  );
  const ticketPhase = experiencePhases[phaseIndex];
  if (!ticketPhase) return null;

  return (
    <main className="relative h-dvh w-screen overflow-hidden bg-[#252a2e]">
      <StageFrame
        phase={ticketPhase}
        phaseIndex={phaseIndex}
        totalPhases={experiencePhases.length}
      >
        <TicketCard
          snapshot={snapshot}
          changeCard={changeCard}
          qrPayload="http://localhost:3000/download/preview-session"
        />
      </StageFrame>
    </main>
  );
}

function TicketFact({
  label,
  value,
  inline = false,
  density,
}: {
  label: string;
  value: string;
  inline?: boolean;
  density: TicketDensity;
}) {
  const valueSize =
    density === "tight" || value.length > 44 ? "text-sm" : "text-base";

  return (
    <div
      className={`min-w-0 ${inline ? "grid grid-cols-[7rem_minmax(0,1fr)] items-baseline gap-4" : ""}`}
    >
      <dt className="text-sm font-light text-[#f0eadf]/55">{label}</dt>
      <dd
        className={`${inline ? "" : "mt-1.5"} leading-snug font-extralight break-keep text-[#f0eadf]/90 ${valueSize}`}
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
  accentColor,
}: {
  number: string;
  label: string;
  value: string;
  density: TicketDensity;
  accentColor: string;
}) {
  const valueSize =
    density === "tight" || value.length > 55
      ? "text-base"
      : density === "compact"
        ? "text-lg"
        : "text-xl";

  return (
    <li className="grid grid-cols-[2.75rem_minmax(0,1fr)] gap-x-4 py-5">
      <span
        className="pt-0.5 text-sm font-light"
        style={{ color: accentColor }}
      >
        {number}
      </span>
      <div className="min-w-0">
        <p className="text-sm font-light text-[#f0eadf]/55">{label}</p>
        <p
          className={`mt-2 leading-relaxed font-extralight break-keep text-[#f0eadf]/90 ${valueSize}`}
        >
          {value}
        </p>
      </div>
    </li>
  );
}

type TicketDensity = "comfortable" | "compact" | "tight";

function getSituationAccent(situationLabel: string): string {
  if (situationLabel === "소개팅") return "#bd8878";
  if (situationLabel === "면접") return "#82a1b6";
  if (situationLabel === "첫 출근") return "#91a486";
  return "#a9a18f";
}

function getTicketDensity(
  snapshot: TicketSnapshot,
  changeCard: ChangeCard,
): TicketDensity {
  const totalLength = [
    snapshot.criticalMoment,
    snapshot.desiredPersonaLabel,
    snapshot.selectedOutfitLabel,
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
    <div className="absolute inset-0 flex flex-col items-center justify-center gap-5 bg-[#252a2e] px-8 text-center">
      <div className="drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]">
        <p className="mb-4 text-xs font-light tracking-[0.34em] text-[#9aa8ad]">
          CHANGE CARD
        </p>
        <h2 className="text-3xl font-extralight tracking-wide text-[#f0eadf] md:text-4xl">
          티켓을 준비하고 있습니다
        </h2>
      </div>
      <StatusLine text={message} error={status === "FAILED"} />
    </div>
  );
}
