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
import type { TicketJobResponse, VideoUploadStatus } from "../types";

const POLL_INTERVAL_MS = 800;
const VIDEO_POLL_TIMEOUT_MS = 30_000;
const TICKET_POLL_TIMEOUT_MS = 45_000;

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
    let cancelVideoPolling = () => {};
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
        update({ status: "UPLOADING_VIDEO", ticket: null, errorMessage: null });
        try {
          await uploadSessionVideo(
            sessionId,
            recording.blob,
            recording.mimeType,
          );
          update({
            status: "WAITING_FOR_VIDEO",
            ticket: null,
            errorMessage: null,
          });
          const poll = startPolling({
            fetch: () => getSessionVideo(sessionId),
            isTerminal: (video) => isVideoTerminal(video.status),
            intervalMs: POLL_INTERVAL_MS,
            timeoutMs: VIDEO_POLL_TIMEOUT_MS,
          });
          cancelVideoPolling = poll.cancel;
          await poll.promise;
        } catch {
          // A recording failure must not prevent a text ticket without a video.
        }
      }
      if (!cancelled) await generateTicket();
    };

    void uploadVideoAndGenerateTicket();
    return () => {
      cancelled = true;
      cancelVideoPolling();
      cancelTicketPolling();
    };
  }, [recording, recorderStatus, sessionId]);

  return state;
}

function isVideoTerminal(status: VideoUploadStatus) {
  return status === "COMPLETED" || status === "FAILED";
}
