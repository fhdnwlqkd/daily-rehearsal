import type { GestureRecognizer } from "@mediapipe/tasks-vision";

/**
 * API 호출 훅이 공통으로 쓰는 상태.
 * IDLE은 사용자 액션으로 시작하는 훅(use-create-* 등)의 초기 상태 —
 * 마운트 즉시 조회하는 훅(use-get-*)은 LOADING에서 시작한다.
 */
export type ApiStatus = "IDLE" | "LOADING" | "READY" | "ERROR";

/** 타입 선택 화면에 뿌릴 상황 타입 카드 하나. 백엔드 스펙과 1:1. */
export interface SituationType {
  /** 상황 타입 식별자(snake_case). POST /sessions에 그대로 사용한다. */
  key: string;
  /** UI 표시용 한글 라벨. */
  label: string;
  /** 제스처 선택 순서(1부터). 백엔드가 이 값 오름차순으로 정렬해 내려준다. */
  gestureOrder: number;
  /** 브리핑 화면 타이틀. */
  briefingTitle: string;
  /** 예시 답변 목록(빈 배열 가능). */
  exampleAnswers: string[];
}

/** GET /api/v1/situation-types 응답(data 알맹이) — 배열이 그대로 온다. */
export type GetSituationTypesResponse = SituationType[];

/** POST /api/v1/sessions 응답(data 알맹이). 이 두 필드만 온다. */
export interface CreateSessionResponse {
  /** 서버가 생성한 세션 UUID. 이후 모든 세션 API 경로에 사용한다. */
  sessionId: string;
  /** 요청한 situation type key 그대로 에코. */
  situationType: string;
}

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
