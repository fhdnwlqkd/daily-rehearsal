"use client";

import { useEffect, useRef } from "react";

interface WebcamBackgroundProps {
  stream: MediaStream | null;
}

export function WebcamBackground({ stream }: WebcamBackgroundProps) {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.srcObject = stream;
    }
  }, [stream]);

  if (!stream) {
    return <div className="absolute inset-0 z-0 bg-black" />;
  }

  return (
    <div className="absolute inset-0 z-0">
      <video
        ref={videoRef}
        autoPlay
        playsInline
        muted
        className="h-full w-full object-cover"
        style={{ transform: "scaleX(-1)" }}
      />
    </div>
  );
}
