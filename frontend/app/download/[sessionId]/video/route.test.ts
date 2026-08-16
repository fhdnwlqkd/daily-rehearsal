import { afterEach, describe, expect, it, vi } from "vitest";
import { GET } from "./route";

describe("video download route", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("streams a completed video with an attachment filename", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        Response.json({
          success: true,
          data: {
            videoUrl: "https://video.example/session.webm",
            status: "COMPLETED",
          },
        }),
      )
      .mockResolvedValueOnce(
        new Response(new Uint8Array([1, 2, 3]), {
          headers: {
            "content-type": "video/webm",
            "content-length": "3",
          },
        }),
      );
    vi.stubGlobal("fetch", fetchMock);

    const response = await GET({} as never, {
      params: Promise.resolve({ sessionId: "session-id" }),
    });

    expect(response.status).toBe(200);
    expect(response.headers.get("content-disposition")).toBe(
      'attachment; filename="daily-rehearsal.webm"',
    );
    expect(response.headers.get("content-length")).toBe("3");
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("does not fetch a video that is still pending", async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(
      Response.json({
        success: true,
        data: {
          videoUrl: "https://video.example/session.webm",
          status: "PENDING",
        },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const response = await GET({} as never, {
      params: Promise.resolve({ sessionId: "session-id" }),
    });

    expect(response.status).toBe(409);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
