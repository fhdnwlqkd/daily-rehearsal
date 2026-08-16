import { describe, expect, it } from "vitest";

import {
  isVideoDownloadAvailable,
  isVideoTerminal,
} from "../../lib/ticket/video-state";
import type { TicketJobResponse, VideoUploadResponse } from "../../types";

const ticket: TicketJobResponse = {
  sessionId: "session-id",
  status: "COMPLETED",
};

describe("mobile video download state", () => {
  it("stops immediately when no upload was accepted", () => {
    expect(isVideoTerminal("NONE")).toBe(true);
    expect(isVideoTerminal("PENDING")).toBe(false);
  });

  it("shows download when either video status or ticket confirms completion", () => {
    const completedVideo: VideoUploadResponse = {
      sessionId: "session-id",
      status: "COMPLETED",
      videoUrl: "https://video.example/rehearsal.webm",
    };
    expect(isVideoDownloadAvailable(completedVideo, ticket)).toBe(true);
    expect(
      isVideoDownloadAvailable(null, { ...ticket, videoAvailable: true }),
    ).toBe(true);
  });
});
