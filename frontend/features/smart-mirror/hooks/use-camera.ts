"use client";

import { useCallback, useEffect, useState } from "react";

export type CameraStatus = "pending" | "granted" | "denied";

// 뷰포트 방향과 무관하게 항상 가로로 요청한다 (#232 결정: 세로 주문안 철회).
// lucy-vton 계열 realtime 모델은 전부 가로 입력(1088×624, 30fps)이 네이티브라
// 세로 스트림을 올리면 변환이 스톨한다(2026-08-20 실검증 — 연결·과금만 발생).
// 부스가 세로 디스플레이여서 "세로 뷰포트 = 모바일" 추론이 성립하지 않으므로
// 방향 분기 없이 단일 가로 규격으로 통일한다 — 세로 화면 + 가로 스트림에서
// Decart가 정상 동작하는 것은 실검증됨. 세로 화면의 object-cover 크롭(얼굴
// 확대)은 감수한다. 1080p 원본을 SDK가 매 프레임 축소하게 두면 업스트림
// 대역폭과 인코딩 부하만 커져 프레임 드롭이 늘어난다.
const CAMERA_CONSTRAINTS: MediaStreamConstraints = {
  video: {
    facingMode: "user",
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
