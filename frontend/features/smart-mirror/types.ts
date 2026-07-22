import type { GestureRecognizer } from "@mediapipe/tasks-vision";

/**
 * API 호출 훅이 공통으로 쓰는 상태.
 * IDLE은 사용자 액션으로 시작하는 훅(use-create-* 등)의 초기 상태 —
 * 마운트 즉시 조회하는 훅(use-get-*)은 LOADING에서 시작한다.
 */
export type ApiStatus = "IDLE" | "LOADING" | "READY" | "ERROR";

/**
 * 타입 선택 화면에 뿌릴 상황 타입 카드 하나. 실서버 응답과 1:1 (2026-07-19 확정).
 * 구 명세의 gestureOrder·briefingTitle·exampleAnswers는 백엔드에서 제거됨 —
 * 브리핑 화면 데이터(타이틀·예시 답변)의 공급처는 브리핑 스테이지 작업 시
 * 백엔드와 다시 확인한다.
 */
export interface SituationType {
  /** 상황 타입 식별자(snake_case). POST /sessions에 그대로 사용한다. */
  situationType: string;
  /** UI 표시용 한글 라벨. */
  label: string;
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

/**
 * 스테이지(=전시 화면이 전환되는 단위) 식별자. 정의·용어는 frontend/CLAUDE.md의
 * "스테이지 용어 사전"이 기준이다. 라운드·턴 같은 반복은 스테이지 내부 상태로
 * 관리하며 여기에 추가하지 않는다 (예: briefing의 재질문, simulation의 턴).
 */
export type ExperiencePhaseId =
  | "type-select"
  | "briefing"
  | "outfit"
  | "simulation"
  | "ticket";

export interface ExperiencePhase {
  id: ExperiencePhaseId;
  label: string;
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
