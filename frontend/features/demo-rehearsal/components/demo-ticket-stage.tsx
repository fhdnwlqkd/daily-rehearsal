"use client";

import { motion } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import { demoTicket } from "../data/ticket";

export function DemoTicketStage() {
  return (
    <motion.section
      className="absolute inset-0 overflow-y-auto bg-[#e9eef1] px-[clamp(16px,4vw,48px)] pt-[clamp(58px,8vh,82px)] pb-24 text-[#172027]"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
    >
      <div className="mx-auto flex min-h-full w-full max-w-[900px] flex-col justify-center">
        <header className="mb-5 flex items-end justify-between gap-6">
          <div>
            <p className="flex items-center gap-2 text-sm font-semibold tracking-[0.14em] text-[#52616b] uppercase">
              <span className="h-2 w-2 rounded-full bg-[#00B0F0]" />
              Rehearsal complete
            </p>
            <h1 className="mt-2 text-[clamp(2rem,5vw,4rem)] leading-none font-semibold tracking-[-0.03em]">
              내일을 위한 티켓
            </h1>
          </div>
          <p className="text-right text-base leading-relaxed text-[#66757f] max-[560px]:hidden">
            오늘의 연습을
            <br />한 장에 담았습니다
          </p>
        </header>

        <article className="overflow-hidden rounded-[28px] border border-[#dce3e7] bg-white shadow-[0_18px_54px_rgba(24,39,49,0.1)]">
          <div className="grid gap-8 px-[clamp(24px,5vw,48px)] py-[clamp(22px,4vh,40px)] md:grid-cols-[1.55fr_1fr]">
            <div>
              <p className="text-sm font-semibold tracking-[0.16em] text-[#00B0F0] uppercase">
                {demoTicket.situationLabel}
              </p>
              <h2 className="mt-2 text-[clamp(1.5rem,3.5vw,2.75rem)] leading-tight font-semibold tracking-[-0.025em] break-keep">
                내일 기억할 세 가지
              </h2>

              <dl className="mt-7 divide-y divide-[#e7ecef] border-y border-[#e7ecef]">
                <TicketRow
                  label="결정적 순간"
                  value={demoTicket.criticalMoment}
                />
                <TicketRow
                  label="목표 인상"
                  value={demoTicket.desiredPersonaLabel}
                />
                <TicketRow
                  label="선택한 스타일"
                  value={demoTicket.selectedOutfitLabel}
                />
              </dl>
            </div>

            <div className="flex flex-col justify-between rounded-2xl bg-[#172027] p-6 text-white">
              <div className="space-y-5">
                <Plan label="오늘의 행동 변화" value={demoTicket.todayAction} />
                <Plan
                  label="내일 유지할 태도"
                  value={demoTicket.tomorrowAttitude}
                />
                <Plan label="IF · THEN" value={demoTicket.ifThenPlan} />
              </div>
              <div className="mt-7 flex items-center gap-4 border-t border-white/15 pt-5">
                <div className="rounded-lg bg-white p-2">
                  <QRCodeSVG
                    value="https://daily-rehearsal.demo/result"
                    size={66}
                  />
                </div>
                <p className="text-sm leading-relaxed text-white/60">
                  오늘의 연습을
                  <br />
                  내일의 행동으로
                </p>
              </div>
            </div>
          </div>
        </article>
      </div>
    </motion.section>
  );
}

function TicketRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="grid grid-cols-[7rem_1fr] gap-4 py-4">
      <dt className="text-sm font-semibold text-[#77858e]">{label}</dt>
      <dd className="text-base leading-relaxed font-medium break-keep">
        {value}
      </dd>
    </div>
  );
}

function Plan({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-semibold tracking-[0.14em] text-[#58c8ef] uppercase">
        {label}
      </p>
      <p className="mt-1.5 text-[15px] leading-relaxed font-light break-keep text-white/90">
        {value}
      </p>
    </div>
  );
}
