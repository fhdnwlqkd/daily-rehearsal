import { StagePlaceholder } from "../shared/stage-placeholder";

/**
 * 2. 브리핑 — 최초 질문(브리핑) + 재질문(follow-up) 라운드 전체를 담당한다.
 * 재질문은 별도 스테이지가 아니라 이 스테이지의 내부 상태다:
 * 답변을 context API로 보내면 followUpQuestions가 오고, 있으면 재질문 라운드로
 * 전환(max_attempt까지), 없으면 다음 스테이지로 넘어간다.
 * TODO(#89 이후): BriefingQuestion·FollowUpRound 하위 컴포넌트 + STT 입력 구현.
 */
export function BriefingStage() {
  return <StagePlaceholder label="브리핑" />;
}
