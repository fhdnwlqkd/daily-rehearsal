"use client";

import { useEffect, useRef } from "react";

export function DemoVideoBackground({
  stream,
  brighten = false,
}: {
  stream: MediaStream | null;
  brighten?: boolean;
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
          className={`h-full w-full object-cover transition-[filter] duration-500 ${
            brighten ? "brightness-[1.12] contrast-[1.03]" : ""
          }`}
          style={{ transform: "scaleX(-1)" }}
        />
      )}
    </div>
  );
}
