"use client";

import { useCallback, useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import type {
  CreateSessionResponse,
  ExperiencePhaseId,
  GestureEngineHandle,
  SituationType,
} from "../types";
import { experiencePhases } from "../data/phases";
import { StageFrame } from "./stage-frame";
import { TypeSelectStage } from "./stages/type-select-stage";
import { BriefingStage } from "./stages/briefing-stage";
import { OutfitStage } from "./stages/outfit-stage";
import { SimulationStage } from "./stages/simulation-stage";
import { TicketStage } from "./stages/ticket-stage";

interface ExperienceSessionProps {
  /** 부스 수명 장비(SmartMirror 소유) — 세션이 리셋돼도 재로딩되지 않는다. */
  engine: GestureEngineHandle;
  stream: MediaStream | null;
  /** 티켓(마지막)에서 복귀 시 호출 — 부모가 key를 바꿔 이 층을 통째로 리마운트한다. */
  onRestart: () => void;
}

/**
 * 세션 수명 층. 한 관람객의 세션 상태(phaseIndex·sessionId·situationType)를
 * 전부 소유하고 스테이지에 props로 내려준다. 초기화는 수동 리셋이 아니라
 * 부모(SmartMirror)의 key 리마운트로 일어나므로, 세션 상태는 반드시
 * 이 컴포넌트 아래에만 둔다. 카메라 stream·제스처 engine 같은
 * 부스 수명 장비는 여기 두지 않는다 (frontend/CLAUDE.md "수명 2층 구조").
 */
export function ExperienceSession({
  engine,
  stream,
  onRestart,
}: ExperienceSessionProps) {
  const [phaseIndex, setPhaseIndex] = useState(0);
  const [session, setSession] = useState<CreateSessionResponse | null>(null);
  // 선택된 타입 전체(briefingTitle·exampleAnswers 포함) — 브리핑 스테이지가 소비한다.
  const [situationType, setSituationType] = useState<SituationType | null>(
    null,
  );
  const [showDebug, setShowDebug] = useState(false);

  const currentPhase = experiencePhases[phaseIndex];
  const isLastPhase = phaseIndex === experiencePhases.length - 1;

  const goToNextPhase = useCallback(() => {
    if (isLastPhase) {
      onRestart();
      return;
    }
    setPhaseIndex((current) => current + 1);
  }, [isLastPhase, onRestart]);

  const handleSessionCreated = useCallback(
    (created: CreateSessionResponse, selected: SituationType) => {
      setSession(created);
      setSituationType(selected);
      goToNextPhase();
    },
    [goToNextPhase],
  );

  // 개발용 진행(클릭/Enter → 다음 스테이지). 타입 선택에서는 스테이지가
  // 입력(하이라이트 이동·확정)을 소유하므로 전역 진행을 끈다 —
  // 세션 없이 다음 화면으로 새는 것을 막는 실제 흐름 게이트이기도 하다.
  const devAdvanceEnabled = currentPhase?.id !== "type-select";

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.repeat) return;

      if ((event.key === " " || event.key === "Enter") && devAdvanceEnabled) {
        event.preventDefault();
        goToNextPhase();
      }

      if (event.key.toLowerCase() === "d") {
        setShowDebug((visible) => !visible);
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [devAdvanceEnabled, goToNextPhase]);

  if (!currentPhase) return null;

  return (
    <div
      className="absolute inset-0 z-10"
      onClick={devAdvanceEnabled ? goToNextPhase : undefined}
      role="presentation"
    >
      <AnimatePresence mode="wait">
        <motion.div
          key={currentPhase.id}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.65, ease: "easeInOut" }}
          className="absolute inset-0"
        >
          <StageFrame
            phase={currentPhase}
            phaseIndex={phaseIndex}
            totalPhases={experiencePhases.length}
          >
            {renderStage(currentPhase.id, {
              engine,
              stream,
              onSessionCreated: handleSessionCreated,
            })}
          </StageFrame>
        </motion.div>
      </AnimatePresence>

      {showDebug && (
        <div className="absolute top-6 right-6 z-50 flex flex-col items-end gap-2">
          <div className="flex gap-2">
            {experiencePhases.map((phase, index) => (
              <motion.button
                key={phase.id}
                onClick={(event) => {
                  event.stopPropagation();
                  setPhaseIndex(index);
                }}
                className={`flex h-10 min-w-10 items-center justify-center rounded-xl border px-3 text-xs backdrop-blur-xl transition-all ${
                  phaseIndex === index
                    ? "border-white/40 bg-white/20 text-white"
                    : "border-white/10 bg-white/5 text-white/60 hover:border-white/20 hover:bg-white/10 hover:text-white/80"
                }`}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
              >
                {index + 1}
              </motion.button>
            ))}
          </div>
          <div className="rounded-lg border border-white/10 bg-black/60 px-3 py-1.5 text-[10px] font-light tracking-wide text-white/60">
            session:{" "}
            {session && situationType
              ? `${situationType.label}(${session.situationType}) · ${session.sessionId.slice(0, 8)}…`
              : "없음"}
          </div>
        </div>
      )}
    </div>
  );
}

interface StageContext {
  engine: GestureEngineHandle;
  stream: MediaStream | null;
  onSessionCreated: (
    session: CreateSessionResponse,
    situationType: SituationType,
  ) => void;
}

// 스테이지 선택과 props 주입은 세션 층의 책임 — StageFrame은 껍데기만 안다.
// switch가 ExperiencePhaseId를 모두 다루는지는 default의 satisfies never가 강제한다.
function renderStage(phaseId: ExperiencePhaseId, context: StageContext) {
  switch (phaseId) {
    case "type-select":
      return (
        <TypeSelectStage
          engine={context.engine}
          stream={context.stream}
          onSessionCreated={context.onSessionCreated}
        />
      );
    case "briefing":
      return <BriefingStage />;
    case "outfit":
      return <OutfitStage />;
    case "simulation":
      return <SimulationStage />;
    case "ticket":
      return <TicketStage />;
    default:
      return phaseId satisfies never;
  }
}
