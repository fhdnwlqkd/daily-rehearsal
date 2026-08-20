"use client";

import { WebcamBackground } from "./webcam-background";
import { StageFrame } from "./stage-frame";
import { TypeSelectStage } from "./stages/type-select-stage";
import { BriefingStage } from "./stages/briefing-stage";
import { OutfitStage } from "./stages/outfit-stage";
import { SimulationStage } from "./stages/simulation-stage";
import { TicketStage } from "./stages/ticket-stage";
import { experiencePhases } from "../data/phases";
import { useCamera } from "../hooks/use-camera";
import { useGestureEngine } from "../hooks/use-gesture-engine";
import type {
  DecartConnectionHandle,
  ExperiencePhaseId,
  GestureEngineHandle,
  SituationType,
} from "../types";

/**
 * 개발 전용(#232) — 스테이지 하나를 실플로우 진행 없이 바로 띄우는 프리뷰.
 * 반응형 레이아웃을 사이즈별로 캡처·비교하는 용도라 카메라·제스처 엔진은
 * 실물을 쓰되(부스와 같은 렌더링), 세션 데이터는 고정 mock을 주입한다.
 * API 호출은 sessionId="preview-session"으로 나가므로 실서버에서는 에러
 * 상태가 보인다 — 스크린샷 스크립트가 네트워크 레벨에서 mock 응답을 꽂는다.
 */

const noop = () => {};

const PREVIEW_SESSION_ID = "preview-session";

const PREVIEW_SITUATION: SituationType = {
  situationType: "blind_date",
  label: "소개팅",
};

const PREVIEW_DECART: DecartConnectionHandle = {
  status: "IDLE",
  remoteStream: null,
  g2gMs: null,
  applyOutfit: noop,
  disconnect: noop,
};

export function StagePreview({ stage }: { stage: ExperiencePhaseId }) {
  const { stream } = useCamera();
  const engine = useGestureEngine();

  const phaseIndex = experiencePhases.findIndex((phase) => phase.id === stage);
  const phase = experiencePhases[phaseIndex];
  if (!phase) return null;

  return (
    <div className="relative h-dvh w-full overflow-hidden bg-black">
      <WebcamBackground stream={stream} />
      <div className="absolute inset-0 z-10">
        <StageFrame
          phase={phase}
          phaseIndex={phaseIndex}
          totalPhases={experiencePhases.length}
        >
          {renderPreviewStage(stage, engine, stream)}
        </StageFrame>
      </div>
    </div>
  );
}

function renderPreviewStage(
  stage: ExperiencePhaseId,
  engine: GestureEngineHandle,
  stream: MediaStream | null,
) {
  switch (stage) {
    case "type-select":
      return (
        <TypeSelectStage
          engine={engine}
          stream={stream}
          onSessionCreated={noop}
        />
      );
    case "briefing":
      return (
        <BriefingStage
          sessionId={PREVIEW_SESSION_ID}
          situationType={PREVIEW_SITUATION}
          engine={engine}
          stream={stream}
          onComplete={noop}
        />
      );
    case "outfit":
      return (
        <OutfitStage
          sessionId={PREVIEW_SESSION_ID}
          engine={engine}
          stream={stream}
          decart={PREVIEW_DECART}
          onComplete={noop}
        />
      );
    case "simulation":
      return (
        <SimulationStage
          sessionId={PREVIEW_SESSION_ID}
          engine={engine}
          stream={stream}
          onStopRecording={noop}
          onStopDecart={noop}
          onComplete={noop}
        />
      );
    case "ticket":
      return (
        <TicketStage
          sessionId={PREVIEW_SESSION_ID}
          recorderStatus="IDLE"
          recording={null}
        />
      );
    default:
      return stage satisfies never;
  }
}
