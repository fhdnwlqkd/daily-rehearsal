import type { GestureRecognizer } from "@mediapipe/tasks-vision";

/**
 * API 호출 훅이 공통으로 쓰는 상태.
 * IDLE은 사용자 액션으로 시작하는 훅(use-create-* 등)의 초기 상태 —
 * 마운트 즉시 조회하는 훅(use-get-*)은 LOADING에서 시작한다.
 */
export type ApiStatus = "IDLE" | "LOADING" | "READY" | "ERROR";

/**
 * 타입 선택 화면에 뿌릴 상황 타입 카드 하나. 실서버 응답과 1:1 (2026-07-19 확정).
 * 구 명세의 gestureOrder는 백엔드에서 제거됨. 브리핑 화면 데이터(타이틀·예시)는
 * 별도 엔드포인트로 부활했다 — BriefingContent 참고 (2026-07-20 실서버 문서 확정).
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

// --- 브리핑 & context 수집 (이슈 #67) ---

/**
 * GET /api/v1/situation-types/{situationType}/briefing 응답(data 알맹이).
 * 기획상 둘 다 화면에 노출한다: briefingTitle이 메인 질문, exampleAnswer는
 * "이렇게 말하면 돼요" 발화 유도용 (scenario.md §3-2).
 */
export interface BriefingContent {
  situationType: string;
  /** 타입별 고정 브리핑 질문 (예: "내일의 소개팅을 짧게 말해주세요") */
  briefingTitle: string;
  /** 예시 답변 — 실서버 명세 기준 단수형이다 (다이어그램의 exampleAnswers 아님) */
  exampleAnswer: string;
}

/**
 * context 수집 파이프라인의 서버 상태 (GET /sessions/{id}/context의 status).
 * NOT_STARTED·EXTRACTING·MERGING은 비종결(계속 폴링),
 * FOLLOW_UP_REQUIRED·COMPLETED·FAILED가 폴링을 끝내는 상태다.
 */
export type ContextStatus =
  | "NOT_STARTED"
  | "EXTRACTING"
  | "FOLLOW_UP_REQUIRED"
  | "MERGING"
  | "COMPLETED"
  | "FAILED";

/** POST /sessions/{id}/briefing·follow-up의 202 응답(data 알맹이). */
export interface SubmitTranscriptResponse {
  sessionId: string;
  /** briefing 제출 직후 EXTRACTING, follow-up 제출 직후 MERGING */
  status: ContextStatus;
}

/**
 * GET /sessions/{id}/context 응답(data 알맹이). context는 불투명 snake_case
 * 덩어리 — 프론트는 status·followUpQuestions만 소비하고 절대 해석하지 않는다.
 * missingSlotKeys·followUpQuestions는 FOLLOW_UP_REQUIRED일 때만 온다
 * (백엔드 NON_NULL 직렬화로 그 외에는 필드 자체가 생략됨).
 */
export interface SessionContextResponse {
  sessionId: string;
  status: ContextStatus;
  context?: Record<string, unknown>;
  missingSlotKeys?: string[];
  followUpQuestions?: string[];
}

/**
 * 브리핑 스테이지 흐름 상태 (BriefingFlowController 소유).
 * IDLE        최초 답변 대기
 * SUBMITTING  POST 전송 중
 * PROCESSING  202 수신, context 폴링 중 (serverStatus로 EXTRACTING/MERGING 구분)
 * FOLLOW_UP   재질문 표시, 추가 답변 대기
 * COMPLETED   종결 — 스테이지가 onComplete를 부른다
 * FAILED      종결 실패 — retry 가능
 */
export type BriefingFlowStatus =
  | "IDLE"
  | "SUBMITTING"
  | "PROCESSING"
  | "FOLLOW_UP"
  | "COMPLETED"
  | "FAILED";

/**
 * SERVER_FAILED 서버가 status=FAILED를 반환
 * TIMEOUT       폴링 데드라인 초과
 * NETWORK       POST 실패 또는 폴링 연속 에러 소진
 */
export type BriefingFlowFailReason = "SERVER_FAILED" | "TIMEOUT" | "NETWORK";

/** BriefingFlowController가 매 변화마다 통지하는 불변 스냅샷 (SttSnapshot과 동형). */
export interface BriefingFlowSnapshot {
  status: BriefingFlowStatus;
  /** PROCESSING 중 문구 분기용 서버 상태. 그 외에는 null. */
  serverStatus: ContextStatus | null;
  /** FOLLOW_UP일 때만 비어있지 않다. 재질문 리스트는 한 번에 전부 표시한다. */
  followUpQuestions: string[];
  /** 0 = 최초 브리핑, 1부터 재질문 라운드. 상한 판단은 백엔드 책임(프론트 무제한). */
  round: number;
  failReason: BriefingFlowFailReason | null;
}

// --- 옷 입히기 & Decart (이슈 #69) ---

/**
 * GET /sessions/{id}/outfits 응답 배열의 항목 하나 (2026-08-05 실서버 검증).
 * 빈 배열 계약은 없다 — 후보 0개면 백엔드가 404 C003을 던진다(설정 오류로 처리).
 */
export interface OutfitCandidate {
  /** outfit 식별자. outfit-spec 조회와 PATCH /outfit에 그대로 사용한다. */
  outfitId: string;
  /** UI 표시용 한글 라벨 (예: "네이비 정장"). */
  label: string;
  /** 옷 썸네일 이미지 URL (CloudFront). */
  thumbnailUrl: string;
  /** 진입 시 자동으로 입혀줄 기본 옷 여부 — 목록에 정확히 하나 있다. */
  defaultOutfit: boolean;
}

/** GET /api/v1/sessions/{id}/outfits 응답(data 알맹이) — 배열이 그대로 온다. */
export type GetOutfitsResponse = OutfitCandidate[];

/**
 * GET /sessions/{id}/outfit-spec 응답(data 알맹이). Decart VTON에 전달할
 * 재료 — 프론트는 해석하지 않고 setImage(referenceImageUrl, {prompt, enhance})로
 * 그대로 넘긴다. model은 연결 시점에 이미 고정되어 있어 소비하지 않는다.
 */
export interface DecartSpec {
  model: string;
  prompt: string;
  referenceImageUrl: string;
  enhance: boolean;
}

/** POST /api/v1/sessions/{id}/decart-token 응답(data 알맹이). */
export interface IssueDecartTokenResponse {
  /** Decart WebRTC 연결용 단기 client token. 세션당 한 번만 발급된다. */
  clientToken: string;
}

/** PATCH /api/v1/sessions/{id}/outfit 응답(data 알맹이). */
export interface ConfirmOutfitResponse {
  sessionId: string;
}

/**
 * Decart 연결 상태 (SDK의 소문자 ConnectionState를 대문자 컨벤션으로 매핑).
 * IDLE        연결 전 (enabled=false 또는 재료 미비)
 * CONNECTING  토큰 발급~WebRTC 수립 중 (SDK connecting/reconnecting 포함)
 * CONNECTED   변환 스트림 수신 중 (SDK connected/generating)
 * CLOSED      정상 종료 (해제 3지점: 언마운트/세션 종료/최대 연결 시간)
 * ERROR       연결 실패 — 전시는 멈추지 않고 원본 거울로 진행한다
 */
export type DecartConnectionStatus =
  | "IDLE"
  | "CONNECTING"
  | "CONNECTED"
  | "CLOSED"
  | "ERROR";

/**
 * useDecartConnection(세션 층 소유)이 스테이지로 내려주는 핸들.
 * 출력 스트림을 세션 층이 소유해야 옷 입히기→시뮬레이션 전환에도 연결이
 * 유지되고, 이후 녹화(#94)가 같은 스트림을 집어갈 수 있다.
 */
export interface DecartConnectionHandle {
  status: DecartConnectionStatus;
  /** Decart가 변환한 출력 스트림. CONNECTED 전에는 null. */
  remoteStream: MediaStream | null;
  /**
   * SDK가 실측한 glass-to-glass 지연(ms) — 녹화(#94)의 A/V 싱크 보정값.
   * 비디오만 이만큼 늦게 도착하므로 마이크를 같은 만큼 지연시켜야 싱크가
   * 맞는다 (#90 PoC 검증). 측정 전에는 null.
   */
  g2gMs: number | null;
  /** 하이라이트된 옷의 스펙을 프리뷰에 적용한다. 연속 호출 시 마지막 것이 이긴다. */
  applyOutfit: (spec: DecartSpec) => void;
}

// --- 시뮬레이션 (이슈 #68) ---

/**
 * POST /sessions/{id}/simulation/start 응답(data 알맹이).
 * 옷 확정(REHEARSAL_READY) 후에만 성공하고, 세션당 한 번만 시작할 수 있다.
 */
export interface SimulationStartResponse {
  sessionId: string;
  /** 시작 직후 항상 1. */
  currentTurn: number;
  /** 성공해야 하는 턴 수 — 실패 재시도는 턴을 늘리지 않는다(성공 횟수 기준). */
  maxTurn: number;
  /** 첫 상대 발화 — AI가 아니라 타입별 고정 문구다(백엔드 registry). */
  opponentLine: string;
}

/**
 * 판정·다음 발화 비동기 작업의 서버 상태. PENDING만 비종결(계속 폴링).
 * FAILED는 AI 호출 실패가 아니라 워커 자체 장애다 — AI 실패는 백엔드가
 * 고정 피드백(fallback=true)·고정 발화로 흡수해 COMPLETED로 내려준다.
 */
export type SimulationJobStatus = "PENDING" | "COMPLETED" | "FAILED";

/** POST /sessions/{id}/simulation/turns/{n}/evaluation의 202 응답(data 알맹이). */
export interface SubmitEvaluationResponse {
  sessionId: string;
  turnNo: number;
  /** 같은 턴 안에서의 시도 번호(1부터). 재시도마다 증가한다. */
  attemptNo: number;
  status: SimulationJobStatus;
}

/**
 * GET /sessions/{id}/simulation/turns/{n}/evaluation 응답(data 알맹이).
 * success·feedback·fallback은 COMPLETED, failureReason은 FAILED일 때만 온다.
 */
export interface TurnEvaluationResponse {
  sessionId: string;
  turnNo: number;
  attemptNo: number;
  status: SimulationJobStatus;
  /** 답변이 판정을 통과했는가 — 통과 시에만 백엔드 currentTurn이 전진한다. */
  success?: boolean;
  feedback?: string;
  /** AI 호출 실패로 백엔드 고정 피드백이 채워졌는지. */
  fallback?: boolean;
  failureReason?: string;
}

/** POST /sessions/{id}/simulation/turns/{n}/next-line의 202 응답(data 알맹이). */
export interface RequestNextLineResponse {
  sessionId: string;
  turnNo: number;
  status: SimulationJobStatus;
}

/**
 * GET /sessions/{id}/simulation/turns/{n}/next-line 응답(data 알맹이).
 * opponentLine은 COMPLETED, failureReason은 FAILED일 때만 온다.
 */
export interface NextLineResponse {
  sessionId: string;
  turnNo: number;
  status: SimulationJobStatus;
  opponentLine?: string;
  failureReason?: string;
}

/**
 * 시뮬레이션 스테이지 흐름 상태 (SimulationFlowController 소유).
 * STARTING   start 요청 중 (성공 시 첫 상대 발화와 함께 ANSWERING)
 * ANSWERING  상대 발화 표시 + 답변 대기. 직전 판정이 실패면 재시도 화면이다
 * EVALUATING 답변 제출~판정 폴링 중
 * NEXT_LINE  판정 성공 후 다음 상대 발화 요청~폴링 중
 * COMPLETED  maxTurn 성공 종결 — 스테이지가 onComplete를 부른다
 * FAILED     통신·서버 장애 종결 — retry 가능
 */
export type SimulationFlowStatus =
  | "STARTING"
  | "ANSWERING"
  | "EVALUATING"
  | "NEXT_LINE"
  | "COMPLETED"
  | "FAILED";

/**
 * SERVER_FAILED next-line 워커 장애(status=FAILED) — retry가 재요청한다
 * TIMEOUT       폴링 데드라인 초과
 * NETWORK       POST 실패 또는 폴링 연속 에러 소진
 * (판정 워커 장애는 FAILED가 아니라 "실패 판정 + 고정 피드백"으로 흡수한다 —
 *  전시 안 멈춤 원칙. 재제출은 서버가 FAILED attempt 뒤 새 시도를 허용한다.)
 */
export type SimulationFlowFailReason = "SERVER_FAILED" | "TIMEOUT" | "NETWORK";

/** 화면에 보여줄 판정 결과 — TurnEvaluationResponse에서 표시 필드만 추린 것. */
export interface SimulationFeedback {
  success: boolean;
  feedback: string;
  /** AI 실패로 백엔드/프론트 고정 문구가 채워진 경우 (연출 구분용). */
  fallback: boolean;
}

/** SimulationFlowController가 매 변화마다 통지하는 불변 스냅샷. */
export interface SimulationFlowSnapshot {
  status: SimulationFlowStatus;
  /** 현재 턴(1부터). start 응답 전에는 0. */
  currentTurn: number;
  /** 성공해야 하는 턴 수. start 응답 전에는 0. */
  maxTurn: number;
  /** 지금 상대가 한 말. start 응답 전에는 null. */
  opponentLine: string | null;
  /** 마지막으로 제출한 답변 — EVALUATING 표시용. */
  transcript: string | null;
  /** 마지막 판정 결과. 새 턴의 상대 발화가 나오면 지운다. */
  evaluation: SimulationFeedback | null;
  failReason: SimulationFlowFailReason | null;
}

// --- Video upload and ticket (#92) ---

export type VideoUploadStatus = "NONE" | "PENDING" | "COMPLETED" | "FAILED";

export interface VideoUploadResponse {
  sessionId: string;
  videoUrl: string | null;
  status: VideoUploadStatus;
}

export type TicketJobStatus = "PENDING" | "COMPLETED" | "FAILED";

export interface TicketSnapshot {
  situationLabel: string;
  criticalMoment: string;
  desiredPersonaLabel: string;
  selectedOutfitLabel: string;
}

export interface ChangeCard {
  todayAction: string;
  tomorrowAttitude: string;
  ifThenPlan: string;
}

export interface TicketJobResponse {
  sessionId: string;
  status: TicketJobStatus;
  snapshot?: TicketSnapshot;
  changeCard?: ChangeCard;
  fallback?: boolean;
  videoUrl?: string;
  videoAvailable?: boolean;
  downloadUrl?: string;
  qrPayload?: string;
  failureMessage?: string;
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
