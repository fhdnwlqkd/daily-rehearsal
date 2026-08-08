import { apiFetch } from "@/lib/api";
import type {
  BriefingContent,
  ConfirmOutfitResponse,
  CreateSessionResponse,
  DecartSpec,
  GetOutfitsResponse,
  GetSituationTypesResponse,
  IssueDecartTokenResponse,
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
