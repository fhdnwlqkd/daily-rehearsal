import { StagePlaceholder } from "../shared/stage-placeholder";

/**
 * 4. 시뮬레이션 — 상황 속 대화를 N턴 연습한다.
 * 턴 반복은 이 스테이지의 내부 상태다: 턴마다 발화→판정/피드백을 받고,
 * 실패하면 같은 턴을 재시도한다. maxTurn(성공 횟수) 도달 판정은 프론트 책임.
 * TODO(#89 이후): 턴 상태머신 + 판정/피드백 API 소비 + STT/TTS 연동.
 */
export function SimulationStage() {
  return <StagePlaceholder label="시뮬레이션" />;
}
