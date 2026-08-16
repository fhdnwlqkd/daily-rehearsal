"use client";

import { useEffect, useRef, useState } from "react";
import { Download } from "lucide-react";
import { toBlob } from "html-to-image";
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
      } else {
        // 폴링이 끝내 종결 상태를 못 받고 포기한 경우 — 마지막으로 본 상태가
        // PENDING이면 그대로 두면 videoPending이 계속 true로 남아 "영상 준비
        // 중" 화면에 영원히 갇힌다. 화면에서는 실패로 간주해 다음 단계(변화
        // 카드만 저장)로 넘어가게 한다.
        setVideo((current) =>
          current && current.status === "PENDING"
            ? {
                ...current,
                status: "FAILED",
                failureReason: "영상 준비가 오래 걸리고 있습니다.",
              }
            : current,
        );
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
  const [cardFile, setCardFile] = useState<File | null>(null);
  const [isSavingVideo, setIsSavingVideo] = useState(false);
  const [videoSaveError, setVideoSaveError] = useState<string | null>(null);
  const [videoFile, setVideoFile] = useState<File | null>(null);

  const videoUrl = video?.videoUrl ?? ticket.videoUrl;
  const videoAvailable = isVideoDownloadAvailable(video, ticket);
  const videoPending = !videoLookupFinished || video?.status === "PENDING";
  const videoFailed = video?.status === "FAILED";

  // 모바일 공유는 버튼 클릭의 사용자 활성화가 살아 있을 때 즉시 호출해야
  // 안정적이다. 화면이 그려진 직후 PNG를 미리 만들어 둔다.
  useEffect(() => {
    const card = ticketCardRef.current;
    if (!card) return;
    const targetCard = card;
    let cancelled = false;

    async function prepareCard() {
      await document.fonts.ready;
      const blob = await toBlob(targetCard, {
        backgroundColor: "#ffffff",
        cacheBust: true,
        pixelRatio: 2,
        filter: (node) =>
          !(
            node instanceof HTMLElement && node.dataset.exportIgnore === "true"
          ),
      });
      if (!blob) throw new Error("empty card image");
      if (!cancelled) {
        setCardFile(
          new File([blob], `daily-rehearsal-${ticket.sessionId}.png`, {
            type: "image/png",
          }),
        );
      }
    }

    void prepareCard().catch(() => {
      if (!cancelled) {
        setCardSaveError(
          "이미지를 만들지 못했습니다. 잠시 후 다시 시도해주세요.",
        );
      }
    });
    return () => {
      cancelled = true;
    };
  }, [ticket.sessionId]);

  // 영상도 공유 버튼을 누르기 전에 받아 둬야 iOS의 사진/비디오 저장 메뉴를
  // 사용자 클릭 순간 바로 열 수 있다.
  useEffect(() => {
    if (!videoAvailable || !videoUrl) return;
    const controller = new AbortController();

    async function prepareVideo() {
      const response = await fetch(
        `/download/${encodeURIComponent(ticket.sessionId)}/video`,
        { signal: controller.signal, cache: "no-store" },
      );
      if (!response.ok)
        throw new Error(`video download failed: ${response.status}`);
      const blob = await response.blob();
      if (blob.size === 0) throw new Error("empty video");
      const contentType = blob.type || "video/webm";
      const extension = contentType.includes("mp4") ? "mp4" : "webm";
      setVideoFile(
        new File([blob], `daily-rehearsal-${ticket.sessionId}.${extension}`, {
          type: contentType,
        }),
      );
    }

    void prepareVideo().catch((error: unknown) => {
      if (error instanceof DOMException && error.name === "AbortError") return;
      setVideoSaveError(
        "영상을 준비하지 못했습니다. 잠시 후 다시 시도해주세요.",
      );
    });
    return () => controller.abort();
  }, [ticket.sessionId, videoAvailable, videoUrl]);

  if (!snapshot || !changeCard) return null;

  const saveCardImage = async () => {
    if (!cardFile || isSavingCard) return;
    setIsSavingCard(true);
    setCardSaveError(null);
    try {
      await shareOrDownload(cardFile, "내일을 위한 변화 카드");
    } catch {
      setCardSaveError("이미지를 저장하지 못했습니다. 다시 시도해주세요.");
    } finally {
      setIsSavingCard(false);
    }
  };

  const saveVideo = async () => {
    if (!videoFile || isSavingVideo) return;
    setIsSavingVideo(true);
    setVideoSaveError(null);
    try {
      await shareOrDownload(videoFile, "Daily Rehearsal 연습 영상");
    } catch {
      setVideoSaveError("영상을 저장하지 못했습니다. 다시 시도해주세요.");
    } finally {
      setIsSavingVideo(false);
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
            onClick={() => void saveCardImage()}
            disabled={!cardFile || isSavingCard}
            className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-[#172027] bg-transparent px-3 text-sm font-semibold text-[#172027] transition-colors active:bg-white/70 disabled:cursor-wait disabled:opacity-55"
          >
            <Download size={17} aria-hidden />
            {!cardFile
              ? "이미지 준비 중…"
              : isSavingCard
                ? "저장 메뉴 여는 중…"
                : "사진으로 저장"}
          </button>

          {videoAvailable && videoUrl ? (
            <button
              type="button"
              onClick={() => void saveVideo()}
              disabled={!videoFile || isSavingVideo}
              className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-[#172027] px-3 text-sm font-semibold text-white"
            >
              <Download size={17} aria-hidden />
              {!videoFile
                ? "영상 준비 중…"
                : isSavingVideo
                  ? "저장 메뉴 여는 중…"
                  : "영상으로 저장"}
            </button>
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

        {(cardSaveError || videoSaveError || videoFailed) && (
          <div className="mt-3 space-y-1 text-center text-sm" role="status">
            {cardSaveError && <p className="text-red-600">{cardSaveError}</p>}
            {videoSaveError && <p className="text-red-600">{videoSaveError}</p>}
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

/** 모바일은 시스템 공유창(사진/비디오 저장)을, PC·미지원 환경은 다운로드를 쓴다. */
async function shareOrDownload(file: File, title: string) {
  if (
    typeof navigator.share === "function" &&
    typeof navigator.canShare === "function" &&
    navigator.canShare({ files: [file] })
  ) {
    try {
      await navigator.share({ files: [file], title });
      return;
    } catch (error) {
      // 사용자가 공유창을 닫은 것은 저장 오류가 아니다.
      if (error instanceof DOMException && error.name === "AbortError") return;
    }
  }

  const objectUrl = URL.createObjectURL(file);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = file.name;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(objectUrl), 1_000);
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
