// 제스처 판별 튜닝 상수. 값은 데모 페이지에서 실측 후 조정한다.
// 좌표는 MediaPipe 정규화 좌표(0~1, 화면 폭 대비), 시간은 ms.

/** 스와이프 판정에 쓰는 손목 x 궤적의 슬라이딩 윈도우 */
export const SWIPE_WINDOW_MS = 300;
/** 윈도우 내 이 거리 이상 이동해야 스와이프 */
export const SWIPE_MIN_DISTANCE = 0.2;
/** 스와이프 발사 후 연타 방지 쿨다운 */
export const SWIPE_COOLDOWN_MS = 500;

/** Open_Palm 분류를 신뢰하는 최소 점수 */
export const PALM_MIN_SCORE = 0.6;
/**
 * CONFIRM 발사에 필요한 손바닥 유지 시간.
 * 1500 → 2500(차징 중 안내를 읽을 시간 확보) → 다시 1500:
 * 실사용 테스트 결과 2500은 확정이 답답하게 느껴짐 (2026-07-23 사용자 결정).
 */
export const PALM_HOLD_DURATION_MS = 1500;
/** CONFIRM 발사 후 재누적 금지 시간 */
export const PALM_REFRACTORY_MS = 1000;
/** 이 속도(x/ms)를 넘으면 "정지"가 아니라서 유지 누적 리셋 — 스와이프와의 충돌 규칙 */
export const PALM_MAX_SPEED = 0.0002;

/** 프레임 처리 연속 실패가 이 횟수에 달하면 루프 중단 + ERROR */
export const RUNTIME_ERROR_LIMIT = 30;

/**
 * 손 인식이 이 시간 이내로 잠깐 끊긴 경우(빠른 스와이프의 모션 블러)
 * 스와이프 궤적을 리셋하지 않고 유지한다. 실측: 스와이프 최고속 구간에서
 * 200~240ms 인식 공백 발생 (2026-07-05 데모 로그).
 */
export const HAND_LOST_GRACE_MS = 250;

/**
 * 스와이프 발사 후 "반대 방향" 스와이프를 무시하는 시간.
 * 스와이프한 손이 제자리로 돌아오는 동작(return stroke)이
 * 반대 방향 스와이프로 오인식되는 것을 막는다.
 */
export const SWIPE_OPPOSITE_COOLDOWN_MS = 800;
