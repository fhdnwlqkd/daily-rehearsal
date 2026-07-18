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

// --- STT (이슈 #31) ---

/**
 * IDLE       대기 (start() 호출 전/확정·취소 후)
 * LISTENING  인식 중 — transcript가 실시간으로 누적된다
 * CANDIDATE  침묵 감지로 발화가 끝났다고 판단 — 확정/재발화 대기
 * ERROR      복구 필요 — errorType으로 원인 구분
 */
export type SttStatus = "IDLE" | "LISTENING" | "CANDIDATE" | "ERROR";

/**
 * UNSUPPORTED 브라우저에 SpeechRecognition 없음 (비복구 → 키보드 fallback)
 * PERMISSION  마이크 권한 거부 (비복구 → 키보드 fallback)
 * NETWORK     인식 서버 연결 실패 (retry 대상)
 * UNKNOWN     그 외 (retry 대상)
 */
export type SttErrorType = "UNSUPPORTED" | "PERMISSION" | "NETWORK" | "UNKNOWN";

/** SttController가 매 변화마다 통지하는 불변 스냅샷. 훅은 이걸 그대로 상태로 쓴다. */
export interface SttSnapshot {
  status: SttStatus;
  transcript: string;
  errorType: SttErrorType | null;
  /** 소비처가 "N회 실패 → 키보드 전환"을 판단하는 근거. confirm 성공 시 0으로 리셋. */
  failCount: number;
}
