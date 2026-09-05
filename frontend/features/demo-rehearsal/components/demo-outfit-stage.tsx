"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  useGestureController,
  type GestureActionEvent,
  type GestureEngineHandle,
} from "@/features/smart-mirror";
import { demoOutfits } from "../data/outfits";
import type { DemoDecartHandle, DemoOutfit } from "../types";
import { DemoGlassPanel, DemoStatus } from "./demo-ui";

export function DemoOutfitStage({
  engine,
  stream,
  decart,
  onConfirm,
}: {
  engine: GestureEngineHandle;
  stream: MediaStream | null;
  decart: DemoDecartHandle;
  onConfirm: (outfit: DemoOutfit) => void;
}) {
  const defaultIndex = demoOutfits.findIndex((outfit) => outfit.defaultOutfit);
  const [highlightIndex, setHighlightIndex] = useState(
    defaultIndex < 0 ? 0 : defaultIndex,
  );
  const confirmedRef = useRef(false);
  const highlighted = demoOutfits[highlightIndex] ?? demoOutfits[0];
  const { applyOutfit } = decart;

  useEffect(() => {
    applyOutfit(highlighted);
  }, [highlighted, applyOutfit]);

  const confirm = useCallback(() => {
    if (confirmedRef.current || decart.status === "CONNECTING") return;
    confirmedRef.current = true;
    onConfirm(highlighted);
  }, [decart.status, highlighted, onConfirm]);

  const handleAction = useCallback(
    (event: GestureActionEvent) => {
      if (event.action === "NEXT") {
        setHighlightIndex((index) =>
          Math.min(demoOutfits.length - 1, index + 1),
        );
      } else if (event.action === "PREV") {
        setHighlightIndex((index) => Math.max(0, index - 1));
      } else {
        confirm();
      }
    },
    [confirm],
  );

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.repeat || event.isComposing) return;

      if (event.key === "ArrowRight") {
        event.preventDefault();
        setHighlightIndex((index) =>
          Math.min(demoOutfits.length - 1, index + 1),
        );
      } else if (event.key === "ArrowLeft") {
        event.preventDefault();
        setHighlightIndex((index) => Math.max(0, index - 1));
      } else if (event.key === "Enter") {
        event.preventDefault();
        confirm();
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [confirm]);

  const { handVisible, confirmProgress } = useGestureController({
    engine,
    stream,
    onAction: handleAction,
  });

  return (
    <div className="flex h-full flex-col items-center px-[clamp(1rem,4vw,2rem)] pt-[clamp(4rem,12vh,6rem)] pb-[clamp(4.5rem,12vh,6rem)]">
      <div className="text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]">
        <p className="mb-3 text-xs font-light tracking-[0.34em] text-white/65">
          TOMORROW&apos;S LOOK
        </p>
        <h1 className="text-[clamp(1.5rem,4vw,2.75rem)] font-extralight tracking-wide">
          발표할 내일의 모습을 입어보세요
        </h1>
      </div>

      <div className="flex flex-1 items-center justify-center">
        {decart.status === "CONNECTING" && (
          <DemoStatus text="발표할 모습을 준비하는 중…" />
        )}
        {decart.status === "ERROR" && (
          <DemoStatus text="옷 입히기 연결이 어렵습니다 — 원본 화면으로 계속할 수 있어요" />
        )}
      </div>

      <div className="flex flex-wrap justify-center gap-[clamp(0.75rem,2vw,1.5rem)]">
        {demoOutfits.map((outfit, index) => {
          const selected = index === highlightIndex;
          return (
            <button
              key={outfit.outfitId}
              type="button"
              onClick={() => {
                if (selected) confirm();
                else setHighlightIndex(index);
              }}
              className={`cursor-pointer transition-transform duration-300 ${selected ? "" : "scale-95"}`}
            >
              <DemoGlassPanel
                className={`relative overflow-hidden px-4 py-4 transition-colors ${
                  selected
                    ? "border-white/60 bg-white/20"
                    : "border-white/15 bg-black/20"
                }`}
              >
                <img
                  src={outfit.imageUrl}
                  alt={outfit.label}
                  className={`h-[clamp(5.5rem,16vh,9rem)] w-[clamp(5.5rem,12vw,9rem)] rounded-2xl bg-white object-contain transition-opacity ${selected ? "" : "opacity-65"}`}
                />
                <p className="mt-3 max-w-36 text-sm font-medium tracking-wide text-white">
                  {outfit.label}
                </p>
                <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-white/10">
                  <div
                    className="h-full rounded-full bg-white/90 transition-[width] duration-100"
                    style={{
                      width: `${selected ? confirmProgress * 100 : 0}%`,
                    }}
                  />
                </div>
              </DemoGlassPanel>
            </button>
          );
        })}
      </div>

      <div className="mt-4 h-7">
        <DemoStatus
          text={
            handVisible
              ? "손바닥을 유지하면 이 모습으로 선택합니다"
              : "스와이프하거나 ←/→로 옷을 바꿔보세요"
          }
        />
      </div>
    </div>
  );
}
