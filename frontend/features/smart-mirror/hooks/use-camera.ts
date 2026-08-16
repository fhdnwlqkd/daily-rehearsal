"use client";

import { useCallback, useEffect, useState } from "react";

export type CameraStatus = "pending" | "granted" | "denied";

const CAMERA_CONSTRAINTS: MediaStreamConstraints = {
  video: {
    facingMode: "user",
    // lucy-vton-latest의 네이티브 입력(1088×624, 30fps)에 맞춘다.
    // 1080p 원본을 SDK가 매 프레임 축소하게 두면 업스트림 대역폭과 인코딩
    // 부하만 커져 프레임 드롭이 늘어난다.
    width: { ideal: 1088 },
    height: { ideal: 624 },
    frameRate: { ideal: 30, max: 30 },
  },
  // 오디오는 미래 STT용으로 취득만 해둔다(현재 소비처 없음).
  audio: true,
};

interface UseCameraResult {
  /** 권한 허용 시의 카메라/마이크 스트림. 그 외에는 null. */
  stream: MediaStream | null;
  status: CameraStatus;
  /** 거부/실패 후 다시 권한을 요청한다. */
  retry: () => void;
}

/**
 * getUserMedia를 단일 소유권으로 한 번만 호출하는 훅.
 * SmartMirror가 이 훅을 들고 status로 권한 가이드를 제어하고,
 * stream을 WebcamBackground에 내려준다(WebcamBackground는 dumb).
 */
export function useCamera(): UseCameraResult {
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [status, setStatus] = useState<CameraStatus>("pending");
  // 재시도 트리거: 증가시키면 effect가 다시 실행된다.
  const [attempt, setAttempt] = useState(0);

  const retry = useCallback(() => {
    setAttempt((current) => current + 1);
  }, []);

  useEffect(() => {
    let cancelled = false;
    let acquired: MediaStream | null = null;

    setStatus("pending");

    async function request() {
      try {
        const media =
          await navigator.mediaDevices.getUserMedia(CAMERA_CONSTRAINTS);

        // 언마운트/재시도로 늦게 resolve된 경우: 스트림 정리하고 종료
        if (cancelled) {
          media.getTracks().forEach((track) => track.stop());
          return;
        }

        acquired = media;
        setStream(media);
        setStatus("granted");
      } catch (error) {
        if (cancelled) return;
        // 카메라/마이크 구분 없이, 어떤 실패든 "권한 필요"로 처리
        console.error("Camera/mic permission denied or unavailable:", error);
        setStream(null);
        setStatus("denied");
      }
    }

    void request();

    return () => {
      cancelled = true;
      acquired?.getTracks().forEach((track) => track.stop());
    };
  }, [attempt]);

  return { stream, status, retry };
}
