/** 판정·다음 발화 폴링 간격. GET은 RDB만 읽어 저렴하므로 백오프 없이 고정. */
export const SIMULATION_POLL_INTERVAL_MS = 1500;

/**
 * 폴링 데드라인. LLM 판정·발화 생성이 느릴 수 있어 브리핑과 같게 잡았다 —
 * 실서버 소요 실측 후 튜닝할 것.
 */
export const SIMULATION_POLL_TIMEOUT_MS = 60_000;

/** 폴링 중 허용하는 연속 실패 횟수 (전시장 와이파이 블립 내성). 성공 시 리셋. */
export const SIMULATION_POLL_MAX_CONSECUTIVE_ERRORS = 3;

/**
 * STT CANDIDATE 진입 후 자동 전송까지의 카운트다운 (브리핑과 같은 리듬).
 * 2500 → 4000: 확정 전 검토 시간이 부족해 상향 (2026-08-11 통합 테스트).
 */
export const SIMULATION_AUTO_CONFIRM_MS = 4000;

/**
 * 판정 워커 장애(status=FAILED) 시 화면에 보여줄 고정 피드백.
 * 백엔드 AI 실패 fallback("Please try again.")과 같은 역할의 프론트판 —
 * 전시는 멈추지 않고 같은 턴 재시도로 흡수한다 (scenario.md §3-5).
 */
export const EVALUATION_FALLBACK_FEEDBACK =
  "잘 전달되지 않았어요 — 다시 한 번 이야기해 볼까요?";

/** COMPLETED 연출(마지막 피드백 + 리허설 완료)을 보여준 뒤 onComplete까지의 여운. */
export const SIMULATION_COMPLETE_LINGER_MS = 3000;

/**
 * 턴 완료(ACCEPTED/FORCED_ADVANCE) 피드백을 다음 발화 요청 전 최소 이만큼 보여준다.
 * 다음 발화 폴링이 빨리 끝나면(특히 fake/캐시 응답) 피드백이 뜨자마자 다음 질문으로
 * 덮여 사라져 버리는 문제 완화 (2026-08-16 관람 피드백).
 */
export const SIMULATION_TURN_FEEDBACK_LINGER_MS = 4000;

/**
 * 턴 진입 시 "몇 턴째 · 어떤 상황인지"를 큰 글씨로 먼저 보여주는 인트로 연출의
 * 노출 시간. 이 시간이 끝나면 fade-out 후 상대 발화·행동 요구·답변 입력으로
 * 전환된다 — 재시도(같은 턴)에는 다시 재생하지 않는다.
 */
export const SIMULATION_INTRO_DURATION_MS = 2600;
