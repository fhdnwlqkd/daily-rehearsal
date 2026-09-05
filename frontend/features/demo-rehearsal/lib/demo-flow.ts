import { DEMO_TIMING, demoSimulationTurns } from "../data/scenario";
import type { DemoFlowSnapshot, DemoOutfit } from "../types";

export const initialDemoFlowSnapshot: DemoFlowSnapshot = {
  phase: "briefing",
  briefingStep: "INITIAL_QUESTION",
  simulationStep: "INTRO",
  simulationTurnIndex: 0,
  selectedOutfit: null,
};

export function timedDemoTransition(snapshot: DemoFlowSnapshot): {
  delayMs: number;
  next: DemoFlowSnapshot;
} | null {
  if (snapshot.phase === "briefing") {
    if (snapshot.briefingStep === "INITIAL_TRANSCRIPT") {
      return {
        delayMs: DEMO_TIMING.transcriptRevealMs,
        next: { ...snapshot, briefingStep: "ANALYZING" },
      };
    }
    if (snapshot.briefingStep === "ANALYZING") {
      return {
        delayMs: DEMO_TIMING.briefingAnalysisMs,
        next: { ...snapshot, briefingStep: "FOLLOW_UP_QUESTION" },
      };
    }
    if (snapshot.briefingStep === "FOLLOW_UP_TRANSCRIPT") {
      return {
        delayMs: DEMO_TIMING.transcriptRevealMs,
        next: { ...snapshot, briefingStep: "MERGING" },
      };
    }
    if (snapshot.briefingStep === "MERGING") {
      return {
        delayMs: DEMO_TIMING.followUpMergeMs,
        next: { ...snapshot, phase: "outfit" },
      };
    }
  }

  if (snapshot.phase === "simulation") {
    if (snapshot.simulationStep === "INTRO") {
      return {
        delayMs: DEMO_TIMING.simulationIntroMs,
        next: { ...snapshot, simulationStep: "ANSWERING" },
      };
    }
    if (snapshot.simulationStep === "TRANSCRIPT") {
      return {
        delayMs: DEMO_TIMING.transcriptRevealMs,
        next: { ...snapshot, simulationStep: "EVALUATING" },
      };
    }
    if (snapshot.simulationStep === "EVALUATING") {
      return {
        delayMs: DEMO_TIMING.evaluationMs,
        next: { ...snapshot, simulationStep: "FEEDBACK" },
      };
    }
  }

  return null;
}

export function advanceDemoFlow(snapshot: DemoFlowSnapshot): DemoFlowSnapshot {
  if (snapshot.phase === "briefing") {
    if (snapshot.briefingStep === "INITIAL_QUESTION") {
      return { ...snapshot, briefingStep: "INITIAL_TRANSCRIPT" };
    }
    if (snapshot.briefingStep === "FOLLOW_UP_QUESTION") {
      return { ...snapshot, briefingStep: "FOLLOW_UP_TRANSCRIPT" };
    }
    return snapshot;
  }

  if (snapshot.phase === "simulation") {
    if (snapshot.simulationStep === "ANSWERING") {
      return { ...snapshot, simulationStep: "TRANSCRIPT" };
    }
    if (snapshot.simulationStep === "FEEDBACK") {
      const nextTurnIndex = snapshot.simulationTurnIndex + 1;
      if (nextTurnIndex >= demoSimulationTurns.length) {
        return { ...snapshot, phase: "ticket" };
      }
      return {
        ...snapshot,
        simulationTurnIndex: nextTurnIndex,
        simulationStep: "INTRO",
      };
    }
  }

  if (snapshot.phase === "ticket") return initialDemoFlowSnapshot;
  return snapshot;
}

export function confirmDemoOutfit(
  snapshot: DemoFlowSnapshot,
  outfit: DemoOutfit,
): DemoFlowSnapshot {
  return {
    ...snapshot,
    phase: "simulation",
    simulationStep: "INTRO",
    simulationTurnIndex: 0,
    selectedOutfit: outfit,
  };
}
