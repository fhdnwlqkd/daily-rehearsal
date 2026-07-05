/** API 호출 훅(use-get-*)이 공통으로 쓰는 상태. */
export type ApiStatus = "LOADING" | "READY" | "ERROR";

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
