"use client";

import { motion } from "framer-motion";
import { Check, QrCode } from "lucide-react";
import { mockExperience } from "../../data/mock-experience";

export function ChangeCardStage() {
  return (
    <div className="relative flex h-full items-center justify-center px-8">
      <div className="absolute inset-0 bg-black/55 backdrop-blur-md" />
      <motion.div
        className="relative z-10 grid w-full max-w-5xl grid-cols-[1.3fr_0.7fr] overflow-hidden rounded-[2rem] border border-white/15 bg-neutral-100 text-neutral-950 shadow-[0_30px_120px_rgba(0,0,0,0.45)]"
        initial={{ opacity: 0, y: 40, rotateX: 8 }}
        animate={{ opacity: 1, y: 0, rotateX: 0 }}
        transition={{ duration: 0.7, ease: "easeOut" }}
      >
        <div className="p-10">
          <div className="mb-10 flex items-center justify-between border-b border-neutral-300 pb-5">
            <div>
              <p className="text-xs font-medium tracking-[0.28em] text-neutral-500">
                CHANGE CARD
              </p>
              <h2 className="mt-2 text-4xl font-light tracking-tight">
                내일의 변화 카드
              </h2>
            </div>
            <span className="rounded-full border border-neutral-300 px-4 py-2 text-sm text-neutral-600">
              P1-DAILY
            </span>
          </div>
          <ChangeRow
            label="오늘 바꿀 행동"
            value={mockExperience.changeAction}
          />
          <ChangeRow
            label="내일 유지할 태도"
            value={mockExperience.changeAttitude}
          />
          <ChangeRow label="If-Then" value={mockExperience.ifThen} />
          <div className="mt-10 flex items-center gap-3 text-neutral-500">
            <Check className="h-5 w-5" strokeWidth={1.6} />
            <span className="text-sm">
              내일의 리스크를 줄이는 행동 변화가 저장되었습니다.
            </span>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center border-l border-dashed border-neutral-300 bg-neutral-950 p-8 text-white">
          <MockQr />
          <div className="mt-6 flex items-center gap-2 text-white/65">
            <QrCode className="h-4 w-4" strokeWidth={1.4} />
            <span className="text-xs font-light tracking-[0.2em]">
              SCAN TO SAVE
            </span>
          </div>
          <p className="mt-3 text-center text-sm leading-relaxed font-light text-white/55">
            개인 폰으로 스캔해 오늘의 변화 카드를 가져갑니다.
          </p>
        </div>
      </motion.div>
    </div>
  );
}

function ChangeRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="border-b border-neutral-200 py-5">
      <p className="text-xs font-medium tracking-[0.22em] text-neutral-500">
        {label}
      </p>
      <p className="mt-2 text-3xl font-light tracking-tight text-neutral-950">
        {value}
      </p>
    </div>
  );
}

function MockQr() {
  const filled = new Set([
    0, 1, 2, 4, 6, 8, 10, 14, 16, 18, 20, 21, 22, 25, 27, 30, 32, 34, 36, 38,
    40, 42, 45, 48, 50, 52, 54, 56, 58, 60, 62, 64, 65, 66, 70, 72, 74, 76, 78,
    80,
  ]);

  return (
    <div className="grid h-48 w-48 grid-cols-9 gap-1 rounded-2xl bg-white p-4">
      {Array.from({ length: 81 }).map((_, index) => (
        <div
          key={index}
          className={`rounded-[2px] ${filled.has(index) ? "bg-neutral-950" : "bg-transparent"}`}
        />
      ))}
    </div>
  );
}
