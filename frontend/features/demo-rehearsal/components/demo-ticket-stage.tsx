"use client";

import { motion } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import { useEffect, useState } from "react";
import { demoTicket } from "../data/ticket";

export function DemoTicketStage() {
  const [qrPayload, setQrPayload] = useState<string>();
  const density = getTicketDensity();

  useEffect(() => {
    setQrPayload(new URL("/demo", window.location.origin).toString());
  }, []);

  return (
    <motion.section
      className="absolute inset-0 overflow-hidden bg-[#e9eef1] text-[#172027]"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5, ease: "easeOut" }}
    >
      <div className="h-full overflow-y-auto px-[clamp(16px,4vw,48px)] pt-[clamp(14px,2.5vh,28px)] pb-[clamp(58px,6vh,72px)] portrait:pb-24 short:pt-3 short:pb-14">
        <div className="mx-auto flex min-h-full w-full max-w-[840px] flex-col short:max-w-[1040px]">
          <header className="flex shrink-0 items-center justify-between gap-6 pb-[clamp(12px,1.8vh,18px)] short:pb-2">
            <div>
              <p className="flex items-center gap-2.5 text-base font-semibold tracking-[0.14em] text-[#52616b] uppercase">
                <span className="h-2 w-2 rounded-full bg-[#00B0F0]" />
                Rehearsal complete
              </p>
              <h2 className="mt-1.5 text-[clamp(24px,4.8vw,52px)] leading-[1.1] font-semibold tracking-[-0.025em] text-[#121a20] short:mt-0.5 short:text-[clamp(22px,7vh,38px)]">
                내일을 위한 티켓
              </h2>
            </div>
            <p className="text-right text-base leading-[1.45] text-[#66757f] max-[480px]:hidden">
              오늘의 연습을
              <br />한 장에 담았습니다
            </p>
          </header>

          <article className="relative flex shrink-0 flex-col overflow-hidden rounded-[24px] border border-[#dce3e7] bg-white shadow-[0_16px_44px_rgba(24,39,49,0.09)]">
            <div className="grid shrink-0 grid-cols-[minmax(0,1fr)_auto] items-center gap-5 px-[clamp(24px,4vw,38px)] py-[clamp(12px,1.8vh,18px)] short:px-8 short:py-2.5">
              <div className="min-w-0">
                <p className="text-sm font-semibold tracking-[0.16em] text-[#73808a] uppercase portrait:text-xs portrait:tracking-[0.1em]">
                  Daily Rehearsal · Result Ticket
                </p>
                <p className="mt-3 text-base font-semibold text-[#00B0F0] short:mt-1.5">
                  {demoTicket.situationLabel}
                </p>
                <h3 className="mt-1 text-[clamp(20px,4.25vw,46px)] leading-[1.12] font-semibold tracking-[-0.025em] break-keep text-[#172027] short:text-[clamp(18px,6.5vh,34px)]">
                  내일 기억할 세 가지
                </h3>
                <p className="mt-2.5 max-w-lg text-[17px] leading-[1.5] text-[#6a7881] short:mt-1">
                  오늘의 리허설에서 찾은 행동을 내일의 장면에 가져가세요.
                </p>
              </div>

              {qrPayload && (
                <div className="shrink-0 border border-[#dce3e7] bg-white p-2.5 portrait:hidden">
                  <QRCodeSVG
                    value={qrPayload}
                    size={96}
                    marginSize={0}
                    bgColor="#ffffff"
                    fgColor="#172027"
                    className="h-[clamp(56px,11vw,96px)] w-[clamp(56px,11vw,96px)]"
                  />
                </div>
              )}
            </div>

            <TicketPerforation />

            <div className="flex flex-col px-[clamp(24px,4vw,38px)] pt-[clamp(14px,2vh,20px)] pb-[clamp(14px,2vh,20px)] short:grid short:grid-cols-[0.85fr_1.5fr] short:px-8 short:pt-2 short:pb-3">
              <dl className="grid shrink-0 grid-cols-2 gap-x-8 gap-y-3.5 border-b border-[#e5eaed] pb-[clamp(14px,1.8vh,18px)] max-[360px]:grid-cols-1 short:gap-y-2.5 short:border-r short:border-b-0 short:pr-7 short:pb-0">
                <TicketFact
                  label="중요한 순간"
                  value={demoTicket.criticalMoment}
                  density={density}
                />
                <TicketFact
                  label="목표 인상"
                  value={demoTicket.desiredPersonaLabel}
                  density={density}
                />
              </dl>

              <ol className="mt-1 flex flex-col divide-y divide-[#e7ecef] short:mt-0 short:pl-7">
                <ChangePlan
                  number="01"
                  label="먼저 바꿀 행동"
                  value={demoTicket.todayAction}
                  density={density}
                />
                <ChangePlan
                  number="02"
                  label="유지할 태도"
                  value={demoTicket.tomorrowAttitude}
                  density={density}
                />
                <ChangePlan
                  number="03"
                  label="막히는 순간에는"
                  value={demoTicket.ifThenPlan}
                  density={density}
                />
              </ol>

              <footer className="flex shrink-0 items-center justify-between gap-5 border-t border-[#e5eaed] pt-[clamp(12px,1.6vh,16px)] short:col-span-2 short:mt-2 short:pt-2">
                <div>
                  <p className="text-base font-semibold text-[#26343d]">
                    결과와 영상 가져가기
                  </p>
                  <p className="mt-1 text-sm leading-relaxed text-[#75828b]">
                    <span className="portrait:hidden">
                      상단 QR을 휴대폰으로 스캔해주세요
                    </span>
                    <span className="hidden portrait:inline">
                      오른쪽 버튼을 눌러주세요
                    </span>
                  </p>
                </div>
                <span className="rounded-full border border-[#cfd9de] px-3.5 py-2 text-sm font-semibold tracking-[0.12em] text-[#53636d] uppercase portrait:hidden">
                  Valid today
                </span>
                {qrPayload && (
                  <a
                    href={qrPayload}
                    target="_blank"
                    rel="noreferrer"
                    className="hidden shrink-0 items-center rounded-full bg-[#00B0F0] px-4 py-2.5 text-sm font-semibold whitespace-nowrap text-white portrait:inline-flex"
                  >
                    영상·결과 받기
                  </a>
                )}
              </footer>
            </div>
          </article>
        </div>
      </div>
    </motion.section>
  );
}

function TicketFact({
  label,
  value,
  density,
}: {
  label: string;
  value: string;
  density: TicketDensity;
}) {
  const valueSize =
    density === "tight" || value.length > 44 ? "text-lg" : "text-xl";
  return (
    <div className="min-w-0">
      <dt className="text-base font-semibold tracking-[0.06em] text-[#6f7e88] uppercase">
        {label}
      </dt>
      <dd
        className={`mt-1.5 leading-[1.4] font-medium break-keep text-[#26343d] ${valueSize}`}
      >
        {value}
      </dd>
    </div>
  );
}

function ChangePlan({
  number,
  label,
  value,
  density,
}: {
  number: string;
  label: string;
  value: string;
  density: TicketDensity;
}) {
  const valueSize =
    density === "tight" || value.length > 55
      ? "text-lg"
      : density === "compact"
        ? "text-[19px]"
        : "text-xl";
  return (
    <li className="grid grid-cols-[2.75rem_minmax(0,1fr)] items-start gap-x-3 py-[clamp(13px,1.8vh,18px)] short:py-2.5">
      <span className="pt-0.5 text-sm font-semibold text-[#00B0F0]">
        {number}
      </span>
      <div className="min-w-0">
        <p className="text-base font-semibold tracking-[0.04em] text-[#6f7e88] uppercase">
          {label}
        </p>
        <p
          className={`mt-1.5 leading-relaxed font-medium break-keep text-[#26343d] ${valueSize}`}
        >
          {value}
        </p>
      </div>
    </li>
  );
}

type TicketDensity = "comfortable" | "compact" | "tight";

function TicketPerforation() {
  return (
    <div className="relative flex h-7 shrink-0 items-center" aria-hidden>
      <span className="absolute left-0 h-7 w-3.5 -translate-x-1/2 rounded-r-full bg-[#e9eef1]" />
      <div className="mx-5 w-full border-t border-dashed border-[#00B0F0]/65" />
      <span className="absolute right-0 h-7 w-3.5 translate-x-1/2 rounded-l-full bg-[#e9eef1]" />
    </div>
  );
}

function getTicketDensity(): TicketDensity {
  const totalLength = [
    demoTicket.criticalMoment,
    demoTicket.desiredPersonaLabel,
    demoTicket.todayAction,
    demoTicket.tomorrowAttitude,
    demoTicket.ifThenPlan,
  ].reduce((sum, value) => sum + value.length, 0);
  if (totalLength > 300) return "tight";
  if (totalLength > 220) return "compact";
  return "comfortable";
}
