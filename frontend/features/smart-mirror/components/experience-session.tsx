"use client";

import { useCallback, useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import type {
  CreateSessionResponse,
  DecartConnectionHandle,
  ExperiencePhaseId,
  GestureEngineHandle,
  SituationType,
} from "../types";
import { experiencePhases } from "../data/phases";
import { useDecartConnection } from "../hooks/use-decart-connection";
import { useScreenCapture } from "../hooks/use-screen-capture";
import type { ScreenCaptureStatus } from "../hooks/use-screen-capture";
import { useSessionRecorder } from "../hooks/use-session-recorder";
import type {
  SessionRecorderStatus,
  SessionRecording,
} from "../hooks/use-session-recorder";
import { DecartMirrorLayer } from "./decart-mirror-layer";
import { StageFrame } from "./stage-frame";
import { GlassPanel } from "./shared/glass-panel";
import { StagePlaceholder } from "./shared/stage-placeholder";
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
  // 선택된 타입 전체(label 포함) — 브리핑 등 후속 스테이지가 소비한다.
  const [situationType, setSituationType] = useState<SituationType | null>(
    null,
  );
  const [showDebug, setShowDebug] = useState(false);
  // 외부 iframe에서는 Decart 연결을 만들지 않는다. 첫 렌더를 보수적으로
  // embedded=true로 시작해 감지가 끝나기 전에 토큰이 발급되는 것도 막는다.
  const [isEmbedded, setIsEmbedded] = useState(true);
  const [showCaptureGate, setShowCaptureGate] = useState(false);
  const [recordingDisabled, setRecordingDisabled] = useState(false);
  const {
    stream: screenStream,
    status: screenCaptureStatus,
    start: startScreenCapture,
    stop: stopScreenCapture,
  } = useScreenCapture();

  useEffect(() => {
    setIsEmbedded(window.self !== window.top);
  }, []);

  const currentPhase = experiencePhases[phaseIndex];
  const isLastPhase = phaseIndex === experiencePhases.length - 1;

  // 옷 입히기~시뮬레이션 구간 — Decart 변환과 녹화가 함께 살아 있는 구간이다.
  const isMirrorPhase =
    currentPhase?.id === "outfit" || currentPhase?.id === "simulation";
  const isRecordablePhase = isMirrorPhase;

  // Decart 변환 연결 — 스테이지가 아니라 세션 층이 소유한다: 스테이지
  // 전환(언마운트)에도 프리뷰가 유지되어야 하고, 녹화가 같은 스트림을 쓴다.
  const decart = useDecartConnection({
    sessionId: session?.sessionId ?? null,
    cameraStream: stream,
    // 카메라가 항상 가로 규격(1088×624)이므로 뷰포트 방향과 무관하게 켠다
    // (#232 결정 — 세로 화면 + 가로 스트림 조합은 실검증됨).
    enabled: isMirrorPhase && !isEmbedded,
  });

  // 화면 녹화(#94) — 현재 탭을 허용한 세션만 옷 선택부터 시뮬레이션까지
  // 실제 보이는 화면을 담는다. 거절한 세션은 녹화 없이 티켓만 발급한다.
  const {
    status: recorderStatus,
    recording,
    stop: stopRecording,
  } = useSessionRecorder({
    enabled: isRecordablePhase && !recordingDisabled,
    screenStream,
    // iframe·세로 뷰포트에서 Decart를 의도적으로 끈 경우에는 연결 실패와 같은
    // 카메라 폴백 경로를 사용해 원본 영상 녹화·업로드를 그대로 유지한다.
    // (IDLE로 두면 녹화 소스 결정이 연결을 기다리며 시작되지 않는다.)
    // iframe에서 Decart를 의도적으로 끈 경우에는 연결 실패와 같은 카메라
    // 폴백 경로를 사용해 원본 영상 녹화·업로드를 그대로 유지한다.
    decartStatus: isEmbedded ? "CLOSED" : decart.status,
    decartStream: decart.remoteStream,
    cameraStream: stream,
    syncDelayMs: decart.g2gMs,
  });

  const goToNextPhase = useCallback(() => {
    if (isLastPhase) {
      onRestart();
      return;
    }
    setPhaseIndex((current) => current + 1);
  }, [isLastPhase, onRestart]);

  const beginScreenCapture = useCallback(async () => {
    const started = await startScreenCapture();
    if (!started) return;
    setShowCaptureGate(false);
    goToNextPhase();
  }, [goToNextPhase, startScreenCapture]);

  const continueWithoutRecording = useCallback(() => {
    setRecordingDisabled(true);
    setShowCaptureGate(false);
    goToNextPhase();
  }, [goToNextPhase]);

  const stopRecordingAndCapture = useCallback(() => {
    stopRecording();
    stopScreenCapture();
  }, [stopRecording, stopScreenCapture]);

  const handleSessionCreated = useCallback(
    (created: CreateSessionResponse, selected: SituationType) => {
      setSession(created);
      setSituationType(selected);
      goToNextPhase();
    },
    [goToNextPhase],
  );

  const handleBriefingComplete = useCallback(() => {
    // 브리핑은 파일에 넣지 않는다. 옷 선택 진입 직전에 세션당 한 번만 묻는다.
    setShowCaptureGate(true);
  }, []);

  // 개발용 진행(클릭/Enter → 다음 스테이지). 자기 입력(확정·재시도 등)을
  // 소유한 스테이지에서는 전역 진행을 끈다 — 안 끄면 Enter 한 번이 스테이지
  // 확정과 스테이지 스킵으로 이중 처리된다. 실제 흐름 게이트이기도 하다
  // (세션·context 없이 다음 화면으로 새는 것을 막는다).
  const stagesOwningInput: ExperiencePhaseId[] = [
    "type-select",
    "briefing",
    "outfit",
    "simulation",
  ];
  const devAdvanceEnabled =
    !currentPhase || !stagesOwningInput.includes(currentPhase.id);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.repeat) return;
      // 텍스트 입력 중 타이핑(브리핑 키보드 fallback 등)이 디버그 토글·진행으로
      // 새지 않게 편집 가능한 대상에서 온 키는 무시한다.
      const target = event.target;
      if (
        target instanceof HTMLInputElement ||
        target instanceof HTMLTextAreaElement ||
        (target instanceof HTMLElement && target.isContentEditable)
      )
        return;

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
      {/* 변환 프리뷰 — 스테이지(아래 AnimatePresence)보다 먼저 그려 밑에 깔린다 */}
      <DecartMirrorLayer stream={decart.remoteStream} />

      {/* 녹화 고지 — 촬영 중임을 관람객에게 알린다 (#94) */}
      {recorderStatus === "RECORDING" && (
        // 세로 모드에서는 슬림 헤더(로고)와 같은 높이라 겹친다 — 한 줄 아래로 내린다(#232).
        <div className="absolute top-6 left-6 z-50 flex items-center gap-2 rounded-full border border-white/10 bg-black/50 px-3 py-1.5 backdrop-blur-xl portrait:top-11 portrait:left-4">
          <motion.span
            className="h-2 w-2 rounded-full bg-red-500"
            animate={{ opacity: [1, 0.35, 1] }}
            transition={{ duration: 1.6, repeat: Infinity, ease: "easeInOut" }}
            aria-hidden
          />
          <span className="text-[10px] font-light tracking-[0.3em] text-white/70">
            REC
          </span>
        </div>
      )}

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
              session,
              situationType,
              decart,
              recorderStatus,
              recording,
              onSimulationStopRecording: stopRecordingAndCapture,
              onSessionCreated: handleSessionCreated,
              onBriefingComplete: handleBriefingComplete,
              onOutfitConfirmed: goToNextPhase,
              onSimulationStopDecart: decart.disconnect,
              onSimulationComplete: goToNextPhase,
            })}
          </StageFrame>
        </motion.div>
      </AnimatePresence>

      <AnimatePresence>
        {showCaptureGate && (
          <ScreenCaptureGate
            status={screenCaptureStatus}
            onStart={() => void beginScreenCapture()}
            onContinue={continueWithoutRecording}
          />
        )}
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
            {` · rec: ${recorderStatus}`}
          </div>
        </div>
      )}
    </div>
  );
}

function ScreenCaptureGate({
  status,
  onStart,
  onContinue,
}: {
  status: ScreenCaptureStatus;
  onStart: () => void;
  onContinue: () => void;
}) {
  const requesting = status === "REQUESTING";
  const [selectedOption, setSelectedOption] = useState<0 | 1>(0);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (requesting || event.repeat) return;

      if (event.key === "ArrowLeft" || event.key === "ArrowUp") {
        event.preventDefault();
        setSelectedOption(0);
        return;
      }
      if (event.key === "ArrowRight" || event.key === "ArrowDown") {
        event.preventDefault();
        setSelectedOption(1);
        return;
      }
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        // 키보드 이벤트에서 곧바로 호출해야 화면 공유 권한 요청이 브라우저의
        // 사용자 입력으로 인정된다.
        if (selectedOption === 0) onStart();
        else onContinue();
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onContinue, onStart, requesting, selectedOption]);

  return (
    <motion.div
      className="absolute inset-0 z-[70] overflow-hidden text-white"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={(event) => event.stopPropagation()}
    >
      {/* 옷 고르기 스테이지와 같은 거울 위 그라데이션·상하 구조를 쓴다. */}
      <div className="absolute inset-0 bg-gradient-to-b from-black/55 via-black/20 to-black/80" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,transparent_0%,rgba(0,0,0,0.18)_54%,rgba(0,0,0,0.7)_100%)]" />

      <div className="relative flex h-full items-center justify-center px-8 py-16">
        <GlassPanel className="w-full max-w-3xl border-white/30 bg-black/45 px-10 py-9 shadow-[0_24px_80px_rgba(0,0,0,0.45)]">
          <div className="text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]">
            <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/65">
              RECORD YOUR REHEARSAL
            </p>
            <h2 className="text-3xl font-extralight tracking-wide md:text-4xl">
              연습 장면을 영상으로 남길까요?
            </h2>
            <p className="mt-5 text-lg leading-relaxed font-light text-white/75">
              옷을 고르는 순간부터 연습이 끝날 때까지
              <br />
              지금 보이는 화면과 목소리를 함께 담아요.
            </p>
            {status === "ERROR" && (
              <p className="mt-4 text-base text-amber-200">
                화면 선택이 취소됐어요. 다시 시도하거나 영상 없이 진행할 수
                있어요.
              </p>
            )}
          </div>

          <div className="mt-8 grid grid-cols-2 gap-5">
            <button
              type="button"
              onClick={() => {
                setSelectedOption(0);
                onStart();
              }}
              onFocus={() => setSelectedOption(0)}
              disabled={requesting}
              className="cursor-pointer text-left disabled:cursor-wait disabled:opacity-60"
            >
              <GlassPanel
                className={`h-full px-6 py-5 transition-colors ${
                  selectedOption === 0
                    ? "border-white/60 bg-white/20"
                    : "border-white/20 bg-black/15"
                }`}
                pulsing={selectedOption === 0 && !requesting}
                pulseColor="rgba(255, 255, 255, 0.35)"
              >
                <span className="text-xs font-light tracking-[0.3em] text-white/60">
                  01
                </span>
                <p className="mt-3 text-xl font-medium tracking-wide">
                  {requesting ? "화면 선택창 여는 중…" : "영상으로 남기기"}
                </p>
                <p className="mt-2 text-sm font-light text-white/65">
                  현재 탭을 선택하면 바로 시작해요
                </p>
              </GlassPanel>
            </button>
            <button
              type="button"
              onClick={() => {
                setSelectedOption(1);
                onContinue();
              }}
              onFocus={() => setSelectedOption(1)}
              disabled={requesting}
              className="cursor-pointer text-left disabled:opacity-50"
            >
              <GlassPanel
                className={`h-full px-6 py-5 transition-colors ${
                  selectedOption === 1
                    ? "border-white/60 bg-white/20"
                    : "border-white/20 bg-black/15"
                }`}
                pulsing={selectedOption === 1}
                pulseColor="rgba(255, 255, 255, 0.35)"
              >
                <span className="text-xs font-light tracking-[0.3em] text-white/60">
                  02
                </span>
                <p className="mt-3 text-xl font-medium tracking-wide">
                  영상 없이 진행하기
                </p>
                <p className="mt-2 text-sm font-light text-white/65">
                  연습 결과 티켓만 발급해요
                </p>
              </GlassPanel>
            </button>
          </div>
          <p className="mt-5 text-center text-sm font-light tracking-[0.12em] text-white/60">
            키보드 ←/→로 고르고 Enter로 선택 · 화면을 터치해도 돼요
          </p>
        </GlassPanel>
      </div>
    </motion.div>
  );
}

interface StageContext {
  engine: GestureEngineHandle;
  stream: MediaStream | null;
  /** 타입 선택 완료 전에는 null — briefing 이후 스테이지가 소비한다. */
  session: CreateSessionResponse | null;
  situationType: SituationType | null;
  /** Decart 연결 핸들(세션 층 소유) — 옷 입히기 스테이지가 소비한다. */
  decart: DecartConnectionHandle;
  recorderStatus: SessionRecorderStatus;
  recording: SessionRecording | null;
  /** Decart 입력 트랙을 끊기 전에 MediaRecorder의 마지막 청크를 확정한다. */
  onSimulationStopRecording: () => void;
  onSessionCreated: (
    session: CreateSessionResponse,
    situationType: SituationType,
  ) => void;
  onBriefingComplete: () => void;
  onOutfitConfirmed: () => void;
  onSimulationStopDecart: () => void;
  onSimulationComplete: () => void;
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
      // 세션 null 가드는 여기서 — 디버그 점프로 세션 없이 진입한 경우다.
      // 스테이지 props는 non-null로 유지해 스테이지 안 가드를 없앤다.
      if (!context.session || !context.situationType) {
        return (
          <StagePlaceholder label="브리핑 (세션 없음 — 타입 선택부터 진행)" />
        );
      }
      return (
        <BriefingStage
          sessionId={context.session.sessionId}
          situationType={context.situationType}
          engine={context.engine}
          stream={context.stream}
          onComplete={context.onBriefingComplete}
        />
      );
    case "outfit":
      // 세션 null 가드는 브리핑과 동일 — 디버그 점프로 세션 없이 진입한 경우.
      if (!context.session) {
        return (
          <StagePlaceholder label="옷 입히기 (세션 없음 — 타입 선택부터 진행)" />
        );
      }
      return (
        <OutfitStage
          sessionId={context.session.sessionId}
          engine={context.engine}
          stream={context.stream}
          decart={context.decart}
          onComplete={context.onOutfitConfirmed}
        />
      );
    case "simulation":
      // 세션 null 가드는 브리핑과 동일 — 디버그 점프로 세션 없이 진입한 경우.
      if (!context.session) {
        return (
          <StagePlaceholder label="시뮬레이션 (세션 없음 — 타입 선택부터 진행)" />
        );
      }
      return (
        <SimulationStage
          sessionId={context.session.sessionId}
          engine={context.engine}
          stream={context.stream}
          onStopRecording={context.onSimulationStopRecording}
          onStopDecart={context.onSimulationStopDecart}
          onComplete={context.onSimulationComplete}
        />
      );
    case "ticket":
      if (!context.session) {
        return <StagePlaceholder label="티켓을 만들 세션이 없습니다" />;
      }
      return (
        <TicketStage
          sessionId={context.session.sessionId}
          recorderStatus={context.recorderStatus}
          recording={context.recording}
        />
      );
    default:
      return phaseId satisfies never;
  }
}
