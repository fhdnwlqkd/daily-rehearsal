export type DemoPhase = "briefing" | "outfit" | "simulation" | "ticket";

export type DemoBriefingStep =
  | "INITIAL_QUESTION"
  | "INITIAL_TRANSCRIPT"
  | "ANALYZING"
  | "FOLLOW_UP_QUESTION"
  | "FOLLOW_UP_TRANSCRIPT"
  | "MERGING";

export type DemoSimulationStep =
  | "INTRO"
  | "ANSWERING"
  | "TRANSCRIPT"
  | "EVALUATING"
  | "FEEDBACK";

export interface DemoSimulationTurn {
  sceneCue: string;
  opponentLine: string;
  actionPrompt: string;
  transcript: string;
  outcome: "COACHING" | "ACCEPTED";
  feedback: string;
}

export interface DemoOutfit {
  outfitId: string;
  label: string;
  imageUrl: string;
  prompt: string;
  enhance: boolean;
  defaultOutfit: boolean;
}

export type DemoDecartStatus =
  | "IDLE"
  | "CONNECTING"
  | "CONNECTED"
  | "CLOSED"
  | "ERROR";

export interface DemoDecartHandle {
  status: DemoDecartStatus;
  remoteStream: MediaStream | null;
  applyOutfit: (outfit: DemoOutfit) => void;
  disconnect: () => void;
}

export interface DemoFlowSnapshot {
  phase: DemoPhase;
  briefingStep: DemoBriefingStep;
  simulationStep: DemoSimulationStep;
  simulationTurnIndex: number;
  selectedOutfit: DemoOutfit | null;
}
