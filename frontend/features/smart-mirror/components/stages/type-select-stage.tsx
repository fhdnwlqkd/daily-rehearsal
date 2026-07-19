"use client";

import { useCallback, useEffect, useState } from "react";
import { GlassPanel } from "../shared/glass-panel";
import { useGetSituationTypes } from "../../hooks/use-get-situation-types";
import { useCreateSession } from "../../hooks/use-create-session";
import type { CreateSessionResponse, SituationType } from "../../types";

interface TypeSelectStageProps {
  /** 세션 생성 성공 시 세션 층으로 올린다 — 스테이지는 세션 상태를 소유하지 않는다. */
  onSessionCreated: (
    session: CreateSessionResponse,
    situationType: SituationType,
  ) => void;
}

/**
 * 1. 타입 선택 — 연습할 상황 타입을 고르고 세션을 생성한다.
 * 입력은 임시로 키보드(←/→ 이동, Enter 확정)다. 제스처 훅(#9)으로 교체할 때
 * 이 파일의 키보드 effect만 갈아끼우면 되도록 선택 상태와 UI를 분리해 뒀다.
 */
export function TypeSelectStage({ onSessionCreated }: TypeSelectStageProps) {
  const { situationTypes, status: listStatus } = useGetSituationTypes();
  const { session, status: createStatus, create } = useCreateSession();
  const [highlightIndex, setHighlightIndex] = useState(0);

  const confirm = useCallback(() => {
    const highlighted = situationTypes[highlightIndex];
    // LOADING(중복 요청)·READY(이미 성공) 중에는 확정을 막는다. ERROR는 재시도 허용.
    if (!highlighted || createStatus === "LOADING" || createStatus === "READY")
      return;
    create(highlighted.key);
  }, [situationTypes, highlightIndex, createStatus, create]);

  // 키보드 임시 입력 — 제스처 교체 지점.
  useEffect(() => {
    if (listStatus !== "READY") return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.repeat) return;

      if (event.key === "ArrowLeft") {
        setHighlightIndex((index) => Math.max(0, index - 1));
      }
      if (event.key === "ArrowRight") {
        setHighlightIndex((index) =>
          Math.min(situationTypes.length - 1, index + 1),
        );
      }
      if (event.key === "Enter") {
        event.preventDefault();
        confirm();
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [listStatus, situationTypes.length, confirm]);

  // 세션 생성 성공 → 선택된 타입과 함께 세션 층으로 올린다.
  // (하이라이트가 아니라 응답의 situationType으로 역참조 — 성공 후
  // 하이라이트가 움직여도 올라가는 타입이 흔들리지 않는다.)
  useEffect(() => {
    if (createStatus !== "READY" || !session) return;

    const selected = situationTypes.find(
      (type) => type.key === session.situationType,
    );
    if (selected) onSessionCreated(session, selected);
  }, [createStatus, session, situationTypes, onSessionCreated]);

  return (
    <div className="flex h-full flex-col items-center justify-center gap-12 px-8">
      <div className="text-center">
        <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/55">
          SELECT SITUATION
        </p>
        <h2 className="text-4xl font-extralight tracking-wide md:text-5xl">
          내일 연습할 상황을 골라주세요
        </h2>
      </div>

      {listStatus === "LOADING" && (
        <StatusLine text="상황 목록을 불러오는 중…" />
      )}
      {listStatus === "ERROR" && (
        <StatusLine text="상황 목록을 불러오지 못했습니다" error />
      )}

      {listStatus === "READY" && (
        <div className="flex flex-wrap justify-center gap-6">
          {situationTypes.map((type, index) => (
            <TypeCard
              key={type.key}
              type={type}
              highlighted={index === highlightIndex}
            />
          ))}
        </div>
      )}

      {createStatus === "LOADING" && <StatusLine text="세션을 준비하는 중…" />}
      {createStatus === "ERROR" && (
        <StatusLine text="세션 생성에 실패했습니다 — Enter로 다시 시도" error />
      )}
    </div>
  );
}

function TypeCard({
  type,
  highlighted,
}: {
  type: SituationType;
  highlighted: boolean;
}) {
  return (
    <GlassPanel
      className={
        highlighted ? "border-white/50 bg-white/15" : "border-white/10"
      }
      pulsing={highlighted}
      pulseColor="rgba(255, 255, 255, 0.35)"
    >
      <div className="flex w-56 flex-col items-center gap-3 text-center">
        <span className="text-xs font-light tracking-[0.3em] text-white/50">
          {String(type.gestureOrder).padStart(2, "0")}
        </span>
        <span className="text-2xl font-extralight tracking-wide">
          {type.label}
        </span>
      </div>
    </GlassPanel>
  );
}

function StatusLine({
  text,
  error = false,
}: {
  text: string;
  error?: boolean;
}) {
  return (
    <p
      className={`text-sm font-light tracking-[0.2em] ${error ? "text-red-300/80" : "text-white/45"}`}
    >
      {text}
    </p>
  );
}
