"use client";

import { useEffect, useState } from "react";
import { Download } from "lucide-react";
import { getTicketGeneration } from "../../apis";
import { startPolling } from "../../lib/polling/poller";
import {
  ticketPreviewData,
  type TicketPreviewSituation,
} from "../../lib/ticket/preview-data";
import type { TicketJobResponse } from "../../types";

interface MobileDownloadPageProps {
  sessionId: string;
}

export function MobileDownloadPage({ sessionId }: MobileDownloadPageProps) {
  const [ticket, setTicket] = useState<TicketJobResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const poll = startPolling({
      fetch: () => getTicketGeneration(sessionId),
      isTerminal: (response) =>
        response.status === "COMPLETED" || response.status === "FAILED",
      intervalMs: 1000,
      timeoutMs: 45_000,
      onUpdate: setTicket,
    });

    void poll.promise.then((result) => {
      if (result.kind !== "TERMINAL") {
        setErrorMessage("티켓 정보를 불러오지 못했습니다.");
        return;
      }
      setTicket(result.value);
      if (result.value.status === "FAILED") {
        setErrorMessage(
          result.value.failureMessage ?? "티켓 발급에 실패했습니다.",
        );
      }
    });

    return poll.cancel;
  }, [sessionId]);

  if (errorMessage) {
    return <MessageView message={errorMessage} />;
  }
  if (
    ticket?.status !== "COMPLETED" ||
    !ticket.snapshot ||
    !ticket.changeCard
  ) {
    return <MessageView message="변화 카드를 불러오고 있습니다." loading />;
  }

  return <TicketDownloadView ticket={ticket} />;
}

/** 실제 세션 없이 QR→모바일 저장 흐름을 확인하는 개발 전용 화면. */
export function TicketDownloadPreview({
  situation,
}: {
  situation: TicketPreviewSituation;
}) {
  const data = ticketPreviewData[situation];
  const ticket: TicketJobResponse = {
    sessionId: "preview-session",
    status: "COMPLETED",
    snapshot: data.snapshot,
    changeCard: data.changeCard,
    videoAvailable: false,
  };

  return <TicketDownloadView ticket={ticket} />;
}

function TicketDownloadView({ ticket }: { ticket: TicketJobResponse }) {
  const { snapshot, changeCard, videoAvailable, videoUrl } = ticket;
  if (!snapshot || !changeCard) return null;

  const downloadText = () => {
    const content = [
      "내일의 변화 카드",
      "",
      `상황: ${snapshot.situationLabel}`,
      `내일의 중요한 순간: ${snapshot.criticalMoment}`,
      `목표 인상: ${snapshot.desiredPersonaLabel}`,
      `선택한 스타일: ${snapshot.selectedOutfitLabel}`,
      "",
      `오늘의 행동 변화: ${changeCard.todayAction}`,
      `내일 유지할 태도: ${changeCard.tomorrowAttitude}`,
      `If-Then: ${changeCard.ifThenPlan}`,
    ].join("\n");
    const href = URL.createObjectURL(
      new Blob([content], { type: "text/plain;charset=utf-8" }),
    );
    const anchor = document.createElement("a");
    anchor.href = href;
    anchor.download = "daily-rehearsal-change-card.txt";
    document.body.appendChild(anchor);
    anchor.click();
    window.setTimeout(() => {
      anchor.remove();
      URL.revokeObjectURL(href);
    }, 1000);
  };

  return (
    <main className="min-h-dvh bg-neutral-950 px-5 py-8 text-white">
      <div className="mx-auto max-w-xl">
        <p className="text-xs tracking-[0.3em] text-white/50">
          DAILY REHEARSAL
        </p>
        <h1 className="mt-4 text-3xl font-light">내일의 변화 카드</h1>

        <section className="mt-8 border border-white/20 bg-white p-6 text-neutral-950">
          <dl className="divide-y divide-neutral-200">
            <MobileFact label="상황" value={snapshot.situationLabel} />
            <MobileFact
              label="내일의 중요한 순간"
              value={snapshot.criticalMoment}
            />
            <MobileFact
              label="목표 인상"
              value={snapshot.desiredPersonaLabel}
            />
            <MobileFact
              label="선택한 스타일"
              value={snapshot.selectedOutfitLabel}
            />
          </dl>
          <div className="mt-8 space-y-6 border-t border-neutral-200 pt-6">
            <MobilePlan
              label="오늘의 행동 변화"
              value={changeCard.todayAction}
            />
            <MobilePlan
              label="내일 유지할 태도"
              value={changeCard.tomorrowAttitude}
            />
            <MobilePlan label="If-Then" value={changeCard.ifThenPlan} />
          </div>
          <button
            type="button"
            onClick={downloadText}
            className="mt-8 inline-flex h-11 items-center gap-2 border border-neutral-900 px-4 text-sm font-medium text-neutral-900"
          >
            <Download size={16} aria-hidden />
            카드 저장
          </button>
        </section>

        <section className="mt-8 border border-white/15 p-5">
          <h2 className="text-lg font-light">리허설 영상</h2>
          {videoAvailable && videoUrl ? (
            <>
              <video
                className="mt-4 aspect-video w-full bg-black"
                controls
                src={videoUrl}
              />
              <a
                className="mt-4 inline-flex h-11 items-center gap-2 border border-white/30 px-4 text-sm text-white"
                href={videoUrl}
                download
              >
                <Download size={16} aria-hidden />
                영상 다운로드
              </a>
            </>
          ) : (
            <p className="mt-3 text-sm leading-6 text-white/55">
              이번 리허설 영상은 준비되지 않았습니다. 변화 카드는 저장할 수
              있습니다.
            </p>
          )}
        </section>
      </div>
    </main>
  );
}

function MobileFact({ label, value }: { label: string; value: string }) {
  return (
    <div className="py-4">
      <dt className="text-xs text-neutral-500">{label}</dt>
      <dd className="mt-1 text-lg leading-relaxed font-light">{value}</dd>
    </div>
  );
}

function MobilePlan({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-neutral-500">{label}</p>
      <p className="mt-2 text-base leading-relaxed font-light">{value}</p>
    </div>
  );
}

function MessageView({
  message,
  loading = false,
}: {
  message: string;
  loading?: boolean;
}) {
  return (
    <main className="flex min-h-dvh items-center justify-center bg-neutral-950 px-8 text-center text-white">
      <div>
        {loading && (
          <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-white/20 border-t-white" />
        )}
        <p className="mt-5 text-lg font-light text-white/75">{message}</p>
      </div>
    </main>
  );
}
