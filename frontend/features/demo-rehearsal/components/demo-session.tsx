"use client";

import { useEffect } from "react";
import type { GestureEngineHandle } from "@/features/smart-mirror";
import { useDemoDecart } from "../hooks/use-demo-decart";
import { useDemoFlow } from "../hooks/use-demo-flow";
import { DemoBriefingStage } from "./demo-briefing-stage";
import { DemoFrame } from "./demo-frame";
import { DemoOutfitStage } from "./demo-outfit-stage";
import { DemoSimulationStage } from "./demo-simulation-stage";
import { DemoTicketStage } from "./demo-ticket-stage";
import { DemoVideoBackground } from "./demo-video-background";

export function DemoSession({
  engine,
  cameraStream,
}: {
  engine: GestureEngineHandle;
  cameraStream: MediaStream | null;
}) {
  const flow = useDemoFlow();
  const { advance, phase } = flow;
  const mirrorEnabled = phase === "outfit" || phase === "simulation";
  const decart = useDemoDecart({ cameraStream, enabled: mirrorEnabled });

  useEffect(() => {
    if (phase === "outfit") return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key !== "Enter" || event.repeat || event.isComposing) return;
      event.preventDefault();
      advance();
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [advance, phase]);

  const visibleStream = decart.remoteStream ?? cameraStream;

  return (
    <div className="absolute inset-0">
      <DemoVideoBackground stream={visibleStream} />
      <div className="absolute inset-0 z-10">
        <DemoFrame phase={flow.phase}>
          {flow.phase === "briefing" && (
            <DemoBriefingStage step={flow.briefingStep} />
          )}
          {flow.phase === "outfit" && (
            <DemoOutfitStage
              engine={engine}
              stream={cameraStream}
              decart={decart}
              onConfirm={flow.confirmOutfit}
            />
          )}
          {flow.phase === "simulation" && (
            <DemoSimulationStage
              turnIndex={flow.simulationTurnIndex}
              step={flow.simulationStep}
            />
          )}
          {flow.phase === "ticket" && <DemoTicketStage />}
        </DemoFrame>
      </div>
    </div>
  );
}
