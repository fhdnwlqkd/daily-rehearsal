/** context 폴링 간격. GET /context는 RDB만 읽어 저렴하므로 백오프 없이 고정. */
export const CONTEXT_POLL_INTERVAL_MS = 1500;

/**
 * 폴링 데드라인. LLM 추출이 느릴 수 있어 넉넉히 잡았다 —
 * 실서버 추출 소요 실측 후 튜닝할 것 (전시장에서 60초 대기는 길다).
 */
export const CONTEXT_POLL_TIMEOUT_MS = 60_000;

/** 폴링 중 허용하는 연속 실패 횟수 (전시장 와이파이 블립 내성). 성공 시 리셋. */
export const CONTEXT_POLL_MAX_CONSECUTIVE_ERRORS = 3;

/** STT CANDIDATE 진입 후 자동 전송까지의 카운트다운 (CONFIRM은 즉시, PREV는 취소). */
export const BRIEFING_AUTO_CONFIRM_MS = 2500;

/** COMPLETED 연출("상황 준비 완료")을 보여준 뒤 onComplete까지의 여운. */
export const BRIEFING_COMPLETE_LINGER_MS = 1200;
