"use client";

import { useCallback, useEffect, useState } from "react";
import { GlassPanel } from "../shared/glass-panel";
import { useGetSituationTypes } from "../../hooks/use-get-situation-types";
import { useCreateSession } from "../../hooks/use-create-session";
import { useGestureController } from "../../hooks/use-gesture-controller";
import type {
  CreateSessionResponse,
  GestureActionEvent,
  GestureEngineHandle,
  SituationType,
} from "../../types";

interface TypeSelectStageProps {
  /** 부스 수명 장비 — 스테이지는 소비만 한다. */
  engine: GestureEngineHandle;
  stream: MediaStream | null;
  /** 세션 생성 성공 시 세션 층으로 올린다 — 스테이지는 세션 상태를 소유하지 않는다. */
  onSessionCreated: (
    session: CreateSessionResponse,
    situationType: SituationType,
  ) => void;
}

/**
 * 1. 타입 선택 — 연습할 상황 타입을 고르고 세션을 생성한다.
 * 입력은 useGestureController 하나로 들어온다: 손 스와이프(NEXT/PREV)로
 * 하이라이트 이동, 팜홀드(CONFIRM)로 확정. 키보드(←/→·Enter)는 컨트롤러에
 * 내장된 병렬 입력(운영자 개입 통로)이라 별도 처리가 없다.
 */
export function TypeSelectStage({
  engine,
  stream,
  onSessionCreated,
}: TypeSelectStageProps) {
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

  const handleAction = useCallback(
    (event: GestureActionEvent) => {
      if (listStatus !== "READY") return;

      if (event.action === "NEXT") {
        setHighlightIndex((index) =>
          Math.min(situationTypes.length - 1, index + 1),
        );
      }
      if (event.action === "PREV") {
        setHighlightIndex((index) => Math.max(0, index - 1));
      }
      if (event.action === "CONFIRM") {
        confirm();
      }
    },
    [listStatus, situationTypes.length, confirm],
  );

  const {
    status: gestureStatus,
    handVisible,
    confirmProgress,
  } = useGestureController({
    engine,
    stream,
    onAction: handleAction,
    // 세션 생성 성공 후에는 인식 루프·키 입력을 정지해 이중 확정을 막는다.
    enabled: createStatus !== "READY",
  });

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
              confirmProgress={index === highlightIndex ? confirmProgress : 0}
            />
          ))}
        </div>
      )}

      {createStatus === "IDLE" && listStatus === "READY" && (
        <GestureHint gestureStatus={gestureStatus} handVisible={handVisible} />
      )}
      {createStatus === "LOADING" && <StatusLine text="세션을 준비하는 중…" />}
      {createStatus === "ERROR" && (
        <StatusLine
          text="세션 생성에 실패했습니다 — 손바닥을 펴거나 Enter로 다시 시도"
          error
        />
      )}
    </div>
  );
}

function GestureHint({
  gestureStatus,
  handVisible,
}: {
  gestureStatus: GestureEngineHandle["status"];
  handVisible: boolean;
}) {
  if (gestureStatus === "LOADING") {
    return <StatusLine text="제스처 인식을 준비하는 중…" />;
  }
  if (gestureStatus === "ERROR") {
    return (
      <StatusLine text="제스처 인식을 사용할 수 없습니다 — 키보드(←/→·Enter)로 진행하세요" />
    );
  }
  return handVisible ? (
    <StatusLine text="좌우로 스와이프해 고르고, 손바닥을 펴 확정하세요" />
  ) : (
    <StatusLine text="카메라에 손을 들어주세요" />
  );
}

function TypeCard({
  type,
  highlighted,
  confirmProgress,
}: {
  type: SituationType;
  highlighted: boolean;
  /** 0~1 팜홀드 진행률 — 하이라이트 카드에만 차오른다. */
  confirmProgress: number;
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
        <div className="h-1 w-full overflow-hidden rounded-full bg-white/10">
          <div
            className="h-full rounded-full bg-white/70 transition-[width] duration-100"
            style={{ width: `${confirmProgress * 100}%` }}
          />
        </div>
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
