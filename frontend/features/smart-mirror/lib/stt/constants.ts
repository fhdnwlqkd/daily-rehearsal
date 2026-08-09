export const STT_LANG = "ko-KR";

/**
 * 마지막 인식 결과 이후 이 시간 동안 침묵이면 발화가 끝났다고 보고
 * CANDIDATE(확정 대기)로 전이한다. 데모에서 실측 튜닝할 값.
 */
export const SILENCE_CONFIRM_MS = 1800;

/**
 * NETWORK/UNKNOWN 에러가 이 횟수에 도달하면 소비처가 키보드 입력으로
 * 전환하도록 권장하는 기준값. 판단 주체는 소비처(스테이지/데모)다.
 */
export const STT_MAX_FAILS_BEFORE_FALLBACK = 2;
