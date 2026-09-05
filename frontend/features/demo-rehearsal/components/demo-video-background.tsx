"use client";

import { useEffect, useRef } from "react";

export function DemoVideoBackground({
  stream,
}: {
  stream: MediaStream | null;
}) {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (videoRef.current) videoRef.current.srcObject = stream;
  }, [stream]);

  return (
    <div className="absolute inset-0 bg-black">
      {stream && (
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
