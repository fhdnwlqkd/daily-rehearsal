"use client";

import { useEffect, useRef, useState } from "react";
import {
  getSessionVideo,
  getTicketGeneration,
  submitTicketGeneration,
  uploadSessionVideo,
} from "../apis";
import { startPolling } from "../lib/polling/poller";
import type {
  SessionRecorderStatus,
  SessionRecording,
} from "./use-session-recorder";
import type { TicketJobResponse } from "../types";

const POLL_INTERVAL_MS = 800;
const TICKET_POLL_TIMEOUT_MS = 45_000;
const VIDEO_STATUS_POLL_INTERVAL_MS = 1500;
const VIDEO_STATUS_POLL_TIMEOUT_MS = 20_000;

/**
 * 영상 업로드(202) 이후 실제 스토리지 저장(S3/local, 백엔드 비동기 워커)이
 * 성공했는지는 티켓 발급 흐름이 기다리지 않는다 — 그래서 실패해도 화면엔
 * 아무 신호가 없다. F12 콘솔에서 원인을 볼 수 있도록 별도로 상태만 조회해
 * 로그를 남긴다 (상태 갱신은 하지 않음 — 진단 전용, UI에 영향 없음).
 */
async function watchVideoUploadStatus(sessionId: string) {
  const poll = startPolling({
    fetch: () => getSessionVideo(sessionId),
    isTerminal: (video) => video.status !== "PENDING",
    intervalMs: VIDEO_STATUS_POLL_INTERVAL_MS,
    timeoutMs: VIDEO_STATUS_POLL_TIMEOUT_MS,
  });
  const result = await poll.promise;
  if (result.kind !== "TERMINAL") {
    console.warn(
      `[video-upload] S3 저장 상태 확인 실패(${result.kind}) — 백엔드가 응답을 안 주거나 타임아웃`,
      { sessionId },
    );
    return;
  }
  if (result.value.status === "FAILED") {
    console.error("[video-upload] S3 저장 실패:", {
      sessionId,
      failureReason: result.value.failureReason ?? "(백엔드가 사유를 안 줌)",
    });
    return;
  }
  console.log("[video-upload] S3 저장 완료:", {
    sessionId,
    videoUrl: result.value.videoUrl,
  });
}

export type TicketFlowStatus =
  | "WAITING_FOR_RECORDING"
  | "UPLOADING_VIDEO"
  | "WAITING_FOR_VIDEO"
  | "GENERATING"
  | "COMPLETED"
  | "FAILED";

interface TicketFlowState {
  status: TicketFlowStatus;
  ticket: TicketJobResponse | null;
  errorMessage: string | null;
}

export function useTicketFlow(
  sessionId: string | null,
  recorderStatus: SessionRecorderStatus,
  recording: SessionRecording | null,
): TicketFlowState {
  const [state, setState] = useState<TicketFlowState>({
    status: "WAITING_FOR_RECORDING",
    ticket: null,
    errorMessage: null,
  });
  const startedRef = useRef(false);

  useEffect(() => {
    if (!sessionId || startedRef.current || recorderStatus === "RECORDING")
      return;

    startedRef.current = true;
    let cancelled = false;
    let cancelTicketPolling = () => {};

    const update = (next: TicketFlowState) => {
      if (!cancelled) setState(next);
    };

    const generateTicket = async () => {
      update({ status: "GENERATING", ticket: null, errorMessage: null });
      try {
        await submitTicketGeneration(sessionId);
        const poll = startPolling({
          fetch: () => getTicketGeneration(sessionId),
          isTerminal: (ticket) =>
            ticket.status === "COMPLETED" || ticket.status === "FAILED",
          intervalMs: POLL_INTERVAL_MS,
          timeoutMs: TICKET_POLL_TIMEOUT_MS,
        });
        cancelTicketPolling = poll.cancel;
        const result = await poll.promise;
        if (result.kind !== "TERMINAL") {
          update({
            status: "FAILED",
            ticket: null,
            errorMessage: "티켓 발급 결과를 확인하지 못했습니다.",
          });
          return;
        }
        if (result.value.status === "FAILED") {
          update({
            status: "FAILED",
            ticket: result.value,
            errorMessage:
              result.value.failureMessage ?? "티켓 발급에 실패했습니다.",
          });
          return;
        }
        update({
          status: "COMPLETED",
          ticket: result.value,
          errorMessage: null,
        });
      } catch {
        update({
          status: "FAILED",
          ticket: null,
          errorMessage: "티켓 발급을 시작하지 못했습니다.",
        });
      }
    };

    const uploadVideoAndGenerateTicket = async () => {
      if (recording) {
        console.log("[video-upload] 업로드 시작:", {
          sessionId,
          mimeType: recording.mimeType,
          sizeBytes: recording.blob.size,
        });
        update({ status: "UPLOADING_VIDEO", ticket: null, errorMessage: null });
        try {
          const response = await uploadSessionVideo(
            sessionId,
            recording.blob,
            recording.mimeType,
          );
          console.log("[video-upload] 업로드 요청 접수(202):", response);
          // 실제 S3 저장은 백엔드 비동기 워커가 처리한다 — 티켓 발급은 기다리지
          // 않되, 콘솔 진단용으로만 결과를 따로 지켜본다.
          void watchVideoUploadStatus(sessionId);
        } catch (error) {
          // A recording failure must not prevent a text ticket without a video.
          console.error(
            "[video-upload] 세션 영상 업로드 요청에 실패했습니다:",
            {
              sessionId,
              mimeType: recording.mimeType,
              sizeBytes: recording.blob.size,
              error,
            },
          );
        }
      } else {
        console.warn("[video-upload] 업로드할 녹화본이 없어 건너뜁니다:", {
          sessionId,
          recorderStatus,
        });
      }
      if (!cancelled) await generateTicket();
    };

    void uploadVideoAndGenerateTicket();
    return () => {
      cancelled = true;
      cancelTicketPolling();
    };
  }, [recording, recorderStatus, sessionId]);

  return state;
}
