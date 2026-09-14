"use client";

import { useCallback, useEffect, useState } from "react";

export type DemoCameraStatus = "PENDING" | "READY" | "ERROR";

export function useDemoCamera() {
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [status, setStatus] = useState<DemoCameraStatus>("PENDING");
  const [attempt, setAttempt] = useState(0);

  const retry = useCallback(() => setAttempt((current) => current + 1), []);

  useEffect(() => {
    let cancelled = false;
    let acquired: MediaStream | null = null;

    setStatus("PENDING");
    navigator.mediaDevices
      .getUserMedia({
        video: {
          facingMode: "user",
          width: { ideal: 1088 },
          height: { ideal: 624 },
          frameRate: { ideal: 30, max: 30 },
        },
        audio: false,
      })
      .then((media) => {
        if (cancelled) {
          media.getTracks().forEach((track) => track.stop());
          return;
        }
        acquired = media;
        setStream(media);
        setStatus("READY");
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        console.error("Demo camera permission denied or unavailable:", error);
        setStream(null);
        setStatus("ERROR");
      });

    return () => {
      cancelled = true;
      acquired?.getTracks().forEach((track) => track.stop());
    };
  }, [attempt]);

  return { stream, status, retry };
}
