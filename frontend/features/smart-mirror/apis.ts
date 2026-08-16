import { apiFetch } from "@/lib/api";
import type {
  BriefingContent,
  ConfirmOutfitResponse,
  CreateSessionResponse,
  DecartSpec,
  GetOutfitsResponse,
  GetSituationTypesResponse,
  IssueDecartTokenResponse,
  NextLineResponse,
  RequestNextLineResponse,
  SessionContextResponse,
  SimulationStartResponse,
  SubmitEvaluationResponse,
  SubmitTranscriptResponse,
  TurnEvaluationResponse,
  TicketJobResponse,
  VideoUploadResponse,
} from "./types";

/**
 * smart-mirror가 백엔드와 주고받는 API 호출 함수 모음.
 * 컴포넌트가 직접 부르지 않는다 — hooks/의 use-get-* 훅이 부른다.
 *
 * multipart 업로드(녹화 영상)는 apiFetch를 거치지 않고 이 파일 안에서
 * fetch를 직접 쓰는 함수로 추가한다. (다음 발화 SSE는 기획 합의로 폐기 —
 * 시뮬레이션도 브리핑과 같은 202+폴링이다.)
 */

export function getSituationTypes() {
  return apiFetch<GetSituationTypesResponse>("/api/v1/situation-types");
}

/** @param situationType situation-types의 situationType 식별자 (예: "date") */
export function createSession(situationType: string) {
  return apiFetch<CreateSessionResponse>("/api/v1/sessions", {
    method: "POST",
    body: JSON.stringify({ situationType }),
  });
}

/** 타입별 고정 브리핑 질문·예시 답변 조회. */
export function getBriefingContent(situationType: string) {
  return apiFetch<BriefingContent>(
    `/api/v1/situation-types/${situationType}/briefing`,
  );
}

/** 브리핑 답변 제출 — 202 응답. 결과는 getSessionContext 폴링으로 확인한다. */
export function submitBriefing(sessionId: string, transcript: string) {
  return apiFetch<SubmitTranscriptResponse>(
    `/api/v1/sessions/${sessionId}/briefing`,
    { method: "POST", body: JSON.stringify({ transcript }) },
  );
}

/** 재질문 답변 제출 — 202 응답. 결과는 getSessionContext 폴링으로 확인한다. */
export function submitFollowUp(sessionId: string, transcript: string) {
  return apiFetch<SubmitTranscriptResponse>(
    `/api/v1/sessions/${sessionId}/follow-up`,
    { method: "POST", body: JSON.stringify({ transcript }) },
  );
}

/** context 수집 상태 조회 — RDB만 읽는 저렴한 호출이라 폴링에 쓴다. */
export function getSessionContext(sessionId: string) {
  return apiFetch<SessionContextResponse>(
    `/api/v1/sessions/${sessionId}/context`,
  );
}

// --- 옷 입히기 & Decart (이슈 #69) ---
// 아래 4종은 모두 세션이 TRANSFORMATION_READY일 때만 성공한다(아니면 409 S002).

/** 옷 후보 목록 조회. 후보 0개는 404 C003 — 빈 배열로 오지 않는다. */
export function getOutfits(sessionId: string) {
  return apiFetch<GetOutfitsResponse>(`/api/v1/sessions/${sessionId}/outfits`);
}

/**
 * Decart client token 발급. 백엔드가 Decart 토큰 API를 호출해 단기 토큰을
 * 중계한다 — 세션당 한 번만 발급되므로 연결 직전에 받아 즉시 사용할 것.
 */
export function issueDecartToken(sessionId: string) {
  return apiFetch<IssueDecartTokenResponse>(
    `/api/v1/sessions/${sessionId}/decart-token`,
    { method: "POST" },
  );
}

/** 옷 스펙 조회 — 순수 조회라 세션 상태를 바꾸지 않는다(스와이프마다 호출 OK). */
export function getOutfitSpec(sessionId: string, outfitId: string) {
  return apiFetch<DecartSpec>(
    `/api/v1/sessions/${sessionId}/outfit-spec?outfitId=${encodeURIComponent(outfitId)}`,
  );
}

/** 옷 확정 — 성공 시 세션이 REHEARSAL_READY로 전이된다(시뮬레이션 진입 조건). */
export function confirmOutfit(sessionId: string, selectedOutfitId: string) {
  return apiFetch<ConfirmOutfitResponse>(
    `/api/v1/sessions/${sessionId}/outfit`,
    { method: "PATCH", body: JSON.stringify({ selectedOutfitId }) },
  );
}

// --- 시뮬레이션 (이슈 #68) ---
// 판정·다음 발화는 202 접수 후 GET 폴링으로 결과를 받는 브리핑과 같은 패턴.
// 모든 호출은 세션의 currentTurn과 turnNo가 일치해야 한다(아니면 409).

/**
 * 시뮬레이션 시작 — 동기 응답으로 첫 상대 발화까지 온다.
 * REHEARSAL_READY(옷 확정 후)에서만, 세션당 한 번만 성공한다.
 */
export function startSimulation(sessionId: string) {
  return apiFetch<SimulationStartResponse>(
    `/api/v1/sessions/${sessionId}/simulation/start`,
    { method: "POST" },
  );
}

/** 전체 제한시간 만료 — 남은 턴을 소진 처리해 티켓 발급이 가능하게 만든다. */
export function finishSimulation(sessionId: string) {
  return apiFetch<undefined>(
    `/api/v1/sessions/${sessionId}/simulation/finish`,
    {
      method: "POST",
    },
  );
}

/**
 * 턴 판정 제출 — 202 응답. 결과는 getTurnEvaluation 폴링으로 확인한다.
 * 진행 중(PENDING)이거나 이미 끝난 시도가 있으면 새로 만들지 않고 그걸 돌려준다.
 */
export function submitTurnEvaluation(
  sessionId: string,
  turnNo: number,
  transcript: string,
) {
  return apiFetch<SubmitEvaluationResponse>(
    `/api/v1/sessions/${sessionId}/simulation/turns/${turnNo}/evaluation`,
    { method: "POST", body: JSON.stringify({ transcript }) },
  );
}

/** 턴 판정 상태 조회 — RDB만 읽는 저렴한 호출이라 폴링에 쓴다. */
export function getTurnEvaluation(sessionId: string, turnNo: number) {
  return apiFetch<TurnEvaluationResponse>(
    `/api/v1/sessions/${sessionId}/simulation/turns/${turnNo}/evaluation`,
  );
}

/**
 * 다음 상대 발화 요청 — 202 응답. 결과는 getNextLine 폴링으로 확인한다.
 * turnNo는 "다음" 턴 번호다(성공한 턴 + 1 — 프론트 계산). 진행 중이거나
 * 완성된 턴이 있으면 그대로 돌려주고, FAILED 턴만 재생성한다.
 */
export function requestNextLine(sessionId: string, turnNo: number) {
  return apiFetch<RequestNextLineResponse>(
    `/api/v1/sessions/${sessionId}/simulation/turns/${turnNo}/next-line`,
    { method: "POST" },
  );
}

/** 다음 상대 발화 상태 조회 — RDB만 읽는 저렴한 호출이라 폴링에 쓴다. */
export function getNextLine(sessionId: string, turnNo: number) {
  return apiFetch<NextLineResponse>(
    `/api/v1/sessions/${sessionId}/simulation/turns/${turnNo}/next-line`,
  );
}

export function uploadSessionVideo(
  sessionId: string,
  recording: Blob,
  mimeType: string,
) {
  const formData = new FormData();
  formData.append("file", recording, `rehearsal.${extensionFor(mimeType)}`);
  return apiFetch<VideoUploadResponse>(`/api/v1/sessions/${sessionId}/video`, {
    method: "POST",
    body: formData,
  });
}

export function getSessionVideo(sessionId: string) {
  return apiFetch<VideoUploadResponse>(`/api/v1/sessions/${sessionId}/video`);
}

export function submitTicketGeneration(sessionId: string) {
  return apiFetch<TicketJobResponse>(`/api/v1/sessions/${sessionId}/ticket`, {
    method: "POST",
  });
}

export function getTicketGeneration(sessionId: string) {
  return apiFetch<TicketJobResponse>(`/api/v1/sessions/${sessionId}/ticket`);
}

function extensionFor(mimeType: string) {
  return mimeType.includes("mp4") ? "mp4" : "webm";
}
