import {
  PALM_HOLD_DURATION_MS,
  PALM_MAX_FRAME_CREDIT_MS,
  PALM_MAX_SPEED,
  PALM_MIN_SCORE,
  PALM_REFRACTORY_MS,
} from "./constants";

export interface PalmHoldInput {
  /** 이 프레임의 Open_Palm 분류 점수 (감지 안 됐으면 0) */
  openPalmScore: number;
  /** 손목 x — 이동 속도 계산용 (움직이는 손바닥은 확정 후보가 아니다) */
  x: number;
  timestampMs: number;
}

export interface PalmHoldOutput {
  /** 0~1 유지 진행률. 차징 바 UI용 */
  progress: number;
  /** 이 프레임에 CONFIRM이 발사되면 true (1회성) */
  confirmed: boolean;
}

const IDLE: PalmHoldOutput = { progress: 0, confirmed: false };

/**
 * "정지 상태의 Open_Palm"이 PALM_HOLD_DURATION_MS 연속 유지되면
 * CONFIRM을 발사한다. 손 모양 변화/이동/사라짐 시 누적은 즉시 0.
 */
export class PalmHoldDetector {
  private heldMs = 0;
  private last: { x: number; t: number } | null = null;
  private refractoryUntil = Number.NEGATIVE_INFINITY;

  reset(): void {
    this.heldMs = 0;
    this.last = null;
  }

  update({ openPalmScore, x, timestampMs }: PalmHoldInput): PalmHoldOutput {
    const prev = this.last;
    this.last = { x, t: timestampMs };

    if (timestampMs < this.refractoryUntil) return IDLE;
    if (!prev) return IDLE;

    const dt = timestampMs - prev.t;
    if (dt <= 0) {
      return {
        progress: this.heldMs / PALM_HOLD_DURATION_MS,
        confirmed: false,
      };
    }

    const speed = Math.abs(x - prev.x) / dt;
    const holding = openPalmScore >= PALM_MIN_SCORE && speed <= PALM_MAX_SPEED;
    if (!holding) {
      this.heldMs = 0;
      return IDLE;
    }

    // 스톨 프레임(무거운 렌더링으로 dt가 수백 ms~수 초)의 가산을 상한으로
    // 자른다 — 안 자르면 실제 유지 시간보다 훨씬 일찍 확정이 발사되고,
    // 큰 dt는 속도 검사(Δx/dt)까지 무력화해 움직이는 손도 통과시킨다.
    this.heldMs += Math.min(dt, PALM_MAX_FRAME_CREDIT_MS);
    if (this.heldMs >= PALM_HOLD_DURATION_MS) {
      this.heldMs = 0;
      this.refractoryUntil = timestampMs + PALM_REFRACTORY_MS;
      return { progress: 0, confirmed: true };
    }
    return { progress: this.heldMs / PALM_HOLD_DURATION_MS, confirmed: false };
  }
}
