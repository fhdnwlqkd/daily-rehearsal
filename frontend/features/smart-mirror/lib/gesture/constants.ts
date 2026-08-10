// 제스처 판별 튜닝 상수. 값은 데모 페이지에서 실측 후 조정한다.
// 좌표는 MediaPipe 정규화 좌표(0~1, 화면 폭 대비), 시간은 ms.

/**
 * 스와이프 판정에 쓰는 손목 x 궤적의 슬라이딩 윈도우.
 * 300 → 450: HAND_LOST_GRACE_MS(400)보다 길어야 끊김 이전 샘플이
 * 윈도우 밖으로 밀려나지 않고 공백 너머로 이어진다 — 두 값은 세트다.
 */
export const SWIPE_WINDOW_MS = 450;
/**
 * 윈도우 내 이 거리 이상 이동해야 스와이프.
 * 0.2 → 0.25: 윈도우 연장(300→450)으로 느린 이동도 거리를 채우기
 * 쉬워진 것을 보정한다.
 */
export const SWIPE_MIN_DISTANCE = 0.25;
/**
 * 스와이프 발사 후 불응기. 이 동안은 판정만 쉬는 게 아니라 궤적 기록
 * 자체를 버린다 — 기록을 유지하면 손이 제자리로 돌아오는 동작(return
 * stroke)이 쌓였다가 만료 순간 반대 방향으로 발사된다 (2026-08-11
 * 통합 테스트에서 "왼쪽 직후 오른쪽" 오발사 관측). 방향별 쿨다운
 * 2종(같은 방향 500 / 반대 방향 800)을 이 값 하나로 통일했다.
 */
export const SWIPE_REFRACTORY_MS = 1000;

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
/**
 * 한 프레임이 팜홀드 유지 시간에 가산할 수 있는 상한.
 * Decart 스트림+MediaPipe 동시 구동 중 메인 스레드가 1초+ 얼면 그 프레임의
 * dt가 통째로 가산돼 바가 20%인데 확정이 발사됐다 (2026-08-08 옷 스테이지
 * 실테스트). 정상 프레임(16~60ms)은 영향 없고 스톨만 잘라낸다.
 */
export const PALM_MAX_FRAME_CREDIT_MS = 100;
/** 이 속도(x/ms)를 넘으면 "정지"가 아니라서 유지 누적 리셋 — 스와이프와의 충돌 규칙 */
export const PALM_MAX_SPEED = 0.0002;

/** 프레임 처리 연속 실패가 이 횟수에 달하면 루프 중단 + ERROR */
export const RUNTIME_ERROR_LIMIT = 30;

/**
 * 손 인식이 이 시간 이내로 잠깐 끊긴 경우(빠른 스와이프의 모션 블러,
 * 손날 방향 회전) 스와이프 궤적을 리셋하지 않고 유지한다.
 * 실측: 스와이프 최고속 구간에서 200~240ms 인식 공백 (2026-07-05 데모
 * 로그). 250 → 400: 화면을 넘기듯 손날이 카메라를 향하는 자연 스와이프는
 * 공백이 더 길다 (PM 피드백 2026-08-11). SWIPE_WINDOW_MS보다 짧게 유지할
 * 것 — 넘으면 공백 이전 샘플이 윈도우 밖으로 밀려나 브리지가 무의미하다.
 */
export const HAND_LOST_GRACE_MS = 400;

/**
 * 손 감지·추적 신뢰도 하한 (MediaPipe 기본 0.5).
 * 화면을 넘기듯 움직이는 손은 손날이 카메라를 향해 추적이 끊긴다 —
 * 문턱을 낮춰 긴가민가한 손을 계속 추적한다 (PM 피드백 2026-08-11).
 * 제스처 분류 게이트(PALM_MIN_SCORE)는 별개라 CONFIRM 판정은
 * 느슨해지지 않는다. 오탐이 늘면 여기부터 되올릴 것.
 */
export const HAND_MIN_DETECTION_CONFIDENCE = 0.3;
export const HAND_MIN_PRESENCE_CONFIDENCE = 0.3;
export const HAND_MIN_TRACKING_CONFIDENCE = 0.3;
