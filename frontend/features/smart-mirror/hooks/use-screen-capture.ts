"use client";

import { useCallback, useEffect, useRef, useState } from "react";

export type ScreenCaptureStatus = "IDLE" | "REQUESTING" | "READY" | "ERROR";

interface UseScreenCaptureResult {
  stream: MediaStream | null;
  status: ScreenCaptureStatus;
  start: () => Promise<boolean>;
  stop: () => void;
}

type DisplayCaptureOptions = DisplayMediaStreamOptions & {
  /** Chromium 힌트 — 선택 창에서 현재 탭을 우선 노출한다. */
  preferCurrentTab?: boolean;
  selfBrowserSurface?: "include" | "exclude";
  surfaceSwitching?: "include" | "exclude";
};

type DisplayCaptureMediaDevices = Omit<MediaDevices, "getDisplayMedia"> & {
  getDisplayMedia?: MediaDevices["getDisplayMedia"];
};

/** 옷 선택부터 시뮬레이션까지 실제로 보이는 탭 화면을 한 번만 캡처한다. */
export function useScreenCapture(): UseScreenCaptureResult {
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [status, setStatus] = useState<ScreenCaptureStatus>("IDLE");
  const streamRef = useRef<MediaStream | null>(null);

  const stop = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    setStream(null);
    setStatus("IDLE");
  }, []);

  const start = useCallback(async () => {
    const mediaDevices = navigator.mediaDevices as DisplayCaptureMediaDevices;
    if (!mediaDevices.getDisplayMedia) {
      setStatus("ERROR");
      return false;
    }

    setStatus("REQUESTING");
    try {
      const options: DisplayCaptureOptions = {
        video: {
          displaySurface: "browser",
          width: { ideal: 1920, max: 1920 },
          height: { ideal: 1080, max: 1080 },
          frameRate: { ideal: 30, max: 30 },
        },
        // 탭 소리 대신 기존 카메라 스트림의 마이크를 녹화기에 붙인다.
        audio: false,
        preferCurrentTab: true,
        selfBrowserSurface: "include",
        surfaceSwitching: "exclude",
      };
      const captured = await mediaDevices.getDisplayMedia(options);
      const videoTrack = captured.getVideoTracks()[0];
      if (!videoTrack) throw new Error("display capture has no video track");

      streamRef.current = captured;
      setStream(captured);
      setStatus("READY");

      videoTrack.addEventListener(
        "ended",
        () => {
          streamRef.current = null;
          setStream(null);
          setStatus("ERROR");
        },
        { once: true },
      );
      return true;
    } catch (error) {
      console.warn("화면 녹화 권한을 받지 못했습니다:", error);
      streamRef.current = null;
      setStream(null);
      setStatus("ERROR");
      return false;
    }
  }, []);

  useEffect(
    () => () => {
      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    },
    [],
  );

  return { stream, status, start, stop };
}
