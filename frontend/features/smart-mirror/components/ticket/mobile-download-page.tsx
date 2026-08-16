"use client";

import { useEffect, useRef, useState } from "react";
import { Download } from "lucide-react";
import { toPng } from "html-to-image";
import { getSessionVideo, getTicketGeneration } from "../../apis";
import { startPolling } from "../../lib/polling/poller";
import {
  isVideoDownloadAvailable,
  isVideoTerminal,
} from "../../lib/ticket/video-state";
import {
  ticketPreviewData,
  type TicketPreviewSituation,
} from "../../lib/ticket/preview-data";
import type { TicketJobResponse, VideoUploadResponse } from "../../types";

const VIDEO_POLL_TIMEOUT_MS = 120_000;

interface MobileDownloadPageProps {
  sessionId: string;
}

export function MobileDownloadPage({ sessionId }: MobileDownloadPageProps) {
  const [ticket, setTicket] = useState<TicketJobResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [video, setVideo] = useState<VideoUploadResponse | null>(null);
  const [videoLookupFinished, setVideoLookupFinished] = useState(false);

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

  useEffect(() => {
    const poll = startPolling({
      fetch: () => getSessionVideo(sessionId),
      isTerminal: (response) => isVideoTerminal(response.status),
      intervalMs: 1000,
      timeoutMs: VIDEO_POLL_TIMEOUT_MS,
      onUpdate: setVideo,
    });

    void poll.promise.then((result) => {
      if (result.kind === "TERMINAL") {
        setVideo(result.value);
      }
      setVideoLookupFinished(true);
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

  return (
    <TicketDownloadView
      ticket={ticket}
      video={video}
      videoLookupFinished={videoLookupFinished}
    />
  );
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

  return (
    <TicketDownloadView
      ticket={ticket}
      video={{
        sessionId: "preview-session",
        videoUrl: null,
        status: "NONE",
      }}
      videoLookupFinished
    />
  );
}

function TicketDownloadView({
  ticket,
  video,
  videoLookupFinished,
}: {
  ticket: TicketJobResponse;
  video: VideoUploadResponse | null;
  videoLookupFinished: boolean;
}) {
  const { snapshot, changeCard } = ticket;
  const ticketCardRef = useRef<HTMLElement>(null);
  const [isSavingCard, setIsSavingCard] = useState(false);
  const [cardSaveError, setCardSaveError] = useState<string | null>(null);

  if (!snapshot || !changeCard) return null;

  const videoUrl = video?.videoUrl ?? ticket.videoUrl;
  const videoAvailable = isVideoDownloadAvailable(video, ticket);
  const videoPending = !videoLookupFinished || video?.status === "PENDING";
  const videoFailed = video?.status === "FAILED";

  const downloadCardImage = async () => {
    const card = ticketCardRef.current;
    if (!card || isSavingCard) return;

    setIsSavingCard(true);
    setCardSaveError(null);
    try {
      await document.fonts.ready;
      const imageUrl = await toPng(card, {
        backgroundColor: "#ffffff",
        cacheBust: true,
        pixelRatio: 2,
        filter: (node) =>
          !(
            node instanceof HTMLElement && node.dataset.exportIgnore === "true"
          ),
      });
      const anchor = document.createElement("a");
      anchor.href = imageUrl;
      anchor.download = `daily-rehearsal-${ticket.sessionId}.png`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
    } catch {
      setCardSaveError(
        "이미지를 만들지 못했습니다. 잠시 후 다시 시도해주세요.",
      );
    } finally {
      setIsSavingCard(false);
    }
  };

  return (
    <main className="h-dvh overflow-y-auto overscroll-y-contain bg-[#e9eef1] px-4 py-6 text-[#172027] [-webkit-overflow-scrolling:touch] sm:px-6 sm:py-10">
      <div className="mx-auto max-w-xl">
        <header className="px-1 pb-5">
          <p className="flex items-center gap-2 text-xs font-semibold tracking-[0.14em] text-[#52616b] uppercase">
            <span className="h-2 w-2 rounded-full bg-[#00B0F0]" />
            Rehearsal complete
          </p>
          <h1 className="mt-2 text-[34px] leading-[1.1] font-semibold tracking-[-0.025em]">
            내일을 위한 티켓
          </h1>
        </header>

        <article
          ref={ticketCardRef}
          className="relative overflow-hidden rounded-[24px] border border-[#dce3e7] bg-white shadow-[0_16px_44px_rgba(24,39,49,0.09)]"
        >
          <div className="px-6 py-4">
            <p className="text-xs font-semibold tracking-[0.16em] text-[#73808a] uppercase">
              Daily Rehearsal · Result Ticket
            </p>
            <p className="mt-2 text-base font-semibold text-[#00B0F0]">
              {snapshot.situationLabel}
            </p>
            <h2 className="mt-1 text-[30px] leading-[1.15] font-semibold tracking-[-0.025em]">
              내일 기억할 세 가지
            </h2>
            <p className="mt-1.5 text-[15px] leading-[1.55] text-[#6a7881]">
              오늘의 리허설에서 찾은 행동을 내일의 장면에 가져가세요.
            </p>
          </div>

          <MobilePerforation />

          <div className="px-6 pt-4 pb-5">
            <dl className="grid grid-cols-2 gap-x-5 gap-y-4 border-b border-[#e5eaed] pb-5">
              <MobileFact label="중요한 순간" value={snapshot.criticalMoment} />
              <MobileFact
                label="목표 인상"
                value={snapshot.desiredPersonaLabel}
              />
            </dl>

            <ol className="divide-y divide-[#e7ecef]">
              <MobilePlan
                number="01"
                label="먼저 바꿀 행동"
                value={changeCard.todayAction}
              />
              <MobilePlan
                number="02"
                label="유지할 태도"
                value={changeCard.tomorrowAttitude}
              />
              <MobilePlan
                number="03"
                label="막히는 순간에는"
                value={changeCard.ifThenPlan}
              />
            </ol>
          </div>
        </article>

        <div className="mt-4 grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={() => void downloadCardImage()}
            disabled={isSavingCard}
            className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-[#172027] bg-transparent px-3 text-sm font-semibold text-[#172027] transition-colors active:bg-white/70 disabled:cursor-wait disabled:opacity-55"
          >
            <Download size={17} aria-hidden />
            {isSavingCard ? "이미지 생성 중…" : "변화 카드 저장"}
          </button>

          {videoAvailable && videoUrl ? (
            <a
              className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-[#172027] px-3 text-sm font-semibold text-white"
              href={`/download/${encodeURIComponent(ticket.sessionId)}/video`}
              download
            >
              <Download size={17} aria-hidden />
              연습 영상 저장
            </a>
          ) : (
            <button
              type="button"
              disabled
              className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-[#aebbc2] bg-transparent px-3 text-sm font-semibold text-[#7a8992] disabled:cursor-wait"
            >
              <Download size={17} aria-hidden />
              {videoPending ? "영상 준비 중" : "영상 저장 불가"}
            </button>
          )}
        </div>

        {(cardSaveError || videoFailed) && (
          <div className="mt-3 space-y-1 text-center text-sm" role="status">
            {cardSaveError && <p className="text-red-600">{cardSaveError}</p>}
            {videoFailed && (
              <p className="text-[#60707a]">
                영상 저장에 실패했습니다. 변화 카드는 저장할 수 있습니다.
              </p>
            )}
          </div>
        )}
      </div>
    </main>
  );
}

function MobileFact({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <dt className="text-sm font-semibold tracking-[0.04em] text-[#6f7e88]">
        {label}
      </dt>
      <dd className="mt-1.5 text-[17px] leading-[1.45] font-medium break-keep text-[#26343d]">
        {value}
      </dd>
    </div>
  );
}

function MobilePlan({
  number,
  label,
  value,
}: {
  number: string;
  label: string;
  value: string;
}) {
  return (
    <li className="grid grid-cols-[2.25rem_minmax(0,1fr)] gap-x-2 py-4">
      <span className="pt-0.5 text-sm font-semibold text-[#00B0F0]">
        {number}
      </span>
      <div className="min-w-0">
        <p className="text-sm font-semibold tracking-[0.03em] text-[#6f7e88]">
          {label}
        </p>
        <p className="mt-1.5 text-[17px] leading-[1.55] font-medium break-keep text-[#26343d]">
          {value}
        </p>
      </div>
    </li>
  );
}

function MobilePerforation() {
  return (
    <div className="relative flex h-7 items-center" aria-hidden>
      <span className="absolute left-0 h-7 w-3.5 -translate-x-1/2 rounded-r-full bg-[#e9eef1]" />
      <div className="mx-5 w-full border-t border-dashed border-[#00B0F0]/65" />
      <span className="absolute right-0 h-7 w-3.5 translate-x-1/2 rounded-l-full bg-[#e9eef1]" />
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
    <main className="flex min-h-dvh items-center justify-center bg-[#e9eef1] px-8 text-center text-[#172027]">
      <div>
        {loading && (
          <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-[#b9c7ce] border-t-[#00B0F0]" />
        )}
        <p className="mt-5 text-lg font-medium text-[#52616b]">{message}</p>
      </div>
    </main>
  );
}
