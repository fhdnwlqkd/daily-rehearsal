"use client"

import { GlassPanel } from "../glass-panel"
import { SkeletonOverlay } from "../skeleton-overlay"

export function StandbyState() {
  return (
    <div className="relative flex h-full w-full flex-col">
      {/* Center: Skeleton Overlay */}
      <div className="flex flex-1 items-center justify-center">
        <SkeletonOverlay />
      </div>

      {/* Bottom Panel */}
      <div className="absolute inset-x-0 bottom-0 flex justify-center px-6 pb-12">
        <GlassPanel pulsing pulseColor="rgba(134, 239, 172, 0.5)">
          <p className="text-center font-extralight tracking-wide text-white/90">
            어깨가 비대칭입니다. 3초간 바른 자세를 유지해주세요.
          </p>
        </GlassPanel>
      </div>
    </div>
  )
}
