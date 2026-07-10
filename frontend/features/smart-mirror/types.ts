import type { GestureRecognizer } from "@mediapipe/tasks-vision";

export type ExperiencePhaseId =
  | "briefing"
  | "context"
  | "transformation"
  | "gesture-fit"
  | "rehearsal"
  | "change-card";

export interface ExperiencePhase {
  id: ExperiencePhaseId;
  label: string;
  timeRange: string;
}

export interface OutfitOption {
  name: string;
  tone: string;
  active: boolean;
}

/**
 * 한 세션의 경험 콘텐츠. 지금은 mock-experience가 채우지만,
 * 추후 STT transcript·AI 생성 결과·외부 컨텍스트 API가 이 형태로 채워야 한다.
 * mock과 실데이터가 공유하는 단일 계약.
 */
export interface ExperienceData {
  transcript: string;
  contextReply: string;
  tags: string[];
  missing: string[];
  followUpQuestions: string[];
  outfits: OutfitOption[];
  persona: string;
  routeRisk: string;
  placeMood: string;
  gestureHint: string;
  aiPrompt: string;
  userReply: string;
  changeAction: string;
  changeAttitude: string;
  ifThen: string;
}

// --- 제스처 (이슈 #9) ---

export type GestureAction = "NEXT" | "PREV" | "CONFIRM";

export interface GestureActionEvent {
  action: GestureAction;
  /** 어느 입력에서 왔는지 (로깅/디버깅용) */
  source: "hand" | "keyboard";
}

export type GestureEngineStatus = "LOADING" | "READY" | "ERROR";

/** useGestureEngine(세션 루트 소유)이 만들어 스테이지로 내려주는 핸들 */
export interface GestureEngineHandle {
  status: GestureEngineStatus;
  /** READY 전에는 null */
  recognizer: GestureRecognizer | null;
}
