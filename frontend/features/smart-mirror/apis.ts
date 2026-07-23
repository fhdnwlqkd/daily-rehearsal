import { apiFetch } from "@/lib/api";
import type {
  BriefingContent,
  CreateSessionResponse,
  GetSituationTypesResponse,
  SessionContextResponse,
  SubmitTranscriptResponse,
} from "./types";

/**
 * smart-mirror가 백엔드와 주고받는 API 호출 함수 모음.
 * 컴포넌트가 직접 부르지 않는다 — hooks/의 use-get-* 훅이 부른다.
 *
 * SSE 스트리밍(시뮬레이션 다음 발화)과 multipart 업로드(녹화 영상)는
 * apiFetch를 거치지 않고 이 파일 안에서 fetch를 직접 쓰는 함수로 추가한다.
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
