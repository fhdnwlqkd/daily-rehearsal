import { StagePlaceholder } from "../shared/stage-placeholder";

/**
 * 5. 티켓 발급 — 오늘의 연습을 요약한 티켓(상황·입어본 모습·내일의 한 문장 + QR)을 발급한다.
 * 화면 클릭 등 액션으로 첫 스테이지로 복귀하며, 복귀 시 세션 상태 전부
 * (sessionId·situationType·수집 context·녹화물)를 초기화한다 — 세션 층 key 리마운트로 구현.
 * TODO(#89 이후): 티켓 UI + 영상 업로드/QR + 복귀 액션 구현.
 */
export function TicketStage() {
  return <StagePlaceholder label="티켓 발급" />;
}
