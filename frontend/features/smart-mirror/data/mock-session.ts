import type { CreateSessionResponse } from "../types";

/** 백엔드 API 문서의 예시 UUID 그대로. */
const MOCK_SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

/**
 * CreateSessionResponse 명세를 만족하는 mock.
 * 실제 백엔드처럼 요청한 situationType을 그대로 에코해야 해서
 * 정적 객체가 아니라 함수다.
 */
export function mockCreateSessionResponse(
  situationType: string,
): CreateSessionResponse {
  return {
    sessionId: MOCK_SESSION_ID,
    situationType,
  };
}
