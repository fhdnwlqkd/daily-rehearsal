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
