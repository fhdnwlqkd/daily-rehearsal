import { StagePlaceholder } from "../shared/stage-placeholder";

/**
 * 1. 타입 선택 — 연습할 상황 타입을 제스처로 고르고 세션을 생성한다.
 * 세션 생성 성공 시 sessionId·situationType을 부모(세션 층)로 올리고 다음 스테이지로 넘어간다.
 * TODO(#89): useGetSituationTypes·useCreateSession 소비 + 타입 카드 UI 구현.
 */
export function TypeSelectStage() {
  return <StagePlaceholder label="타입 선택" />;
}
