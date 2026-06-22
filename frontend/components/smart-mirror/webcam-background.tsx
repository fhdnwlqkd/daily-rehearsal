"use client";

import { useEffect, useRef, useState } from "react";

export function WebcamBackground() {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);

  useEffect(() => {
    let stream: MediaStream | null = null;
    let cancelled = false;

    async function setupCamera() {
      try {
        const acquired = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: "user",
            width: { ideal: 1920 },
            height: { ideal: 1080 },
          },
          audio: true, // Audio is needed for the mirror's listening state
        });

        // 언마운트 후 늦게 resolve된 경우: 스트림 정리하고 종료
        if (cancelled) {
          acquired.getTracks().forEach((track) => track.stop());
          return;
        }

        stream = acquired;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          setHasPermission(true);
        }
      } catch (error) {
        if (cancelled) return;
        console.error("Media permission denied or unavailable:", error);
        setHasPermission(false);
      }
    }

    void setupCamera();

    return () => {
      cancelled = true;
      stream?.getTracks().forEach((track) => track.stop());
    };
  }, []);

  return (
    <div className="absolute inset-0 z-0">
      {hasPermission === false ? (
        <div className="h-full w-full bg-black" />
      ) : (
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="h-full w-full object-cover"
          style={{ transform: "scaleX(-1)" }}
        />
      )}
    </div>
  );
}
