import { StagePlaceholder } from "../shared/stage-placeholder";

/**
 * 3. 옷 입히기 — 상황에 맞는 착장을 변환해 입혀 보고 제스처로 고른다.
 * (구 목업의 transformation + gesture-fit을 병합한 스테이지.)
 * 이 스테이지에서 녹화가 시작되며, REC 인디케이터는 스테이지 내부가 아니라
 * StageFrame(헤더) 레벨에 둔다 — 시뮬레이션까지 이어지기 때문.
 * TODO(#89 이후): 변환 연출 + 제스처 선택 + 녹화 시작 구현.
 */
export function OutfitStage() {
  return <StagePlaceholder label="옷 입히기" />;
}
