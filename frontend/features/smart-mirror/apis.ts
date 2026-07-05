import { apiFetch } from "@/lib/api";
import type { CreateSessionResponse, GetSituationTypesResponse } from "./types";

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

/** @param situationType situation-types의 key 값 (예: "date") */
export function createSession(situationType: string) {
  return apiFetch<CreateSessionResponse>("/api/v1/sessions", {
    method: "POST",
    body: JSON.stringify({ situationType }),
  });
}
