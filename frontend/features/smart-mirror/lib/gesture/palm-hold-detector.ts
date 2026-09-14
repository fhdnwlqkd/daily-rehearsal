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

/**
 * 이 프레임에 누적이 일어났는지, 안 일어났다면 무엇이 막았는지.
 * 확정이 안 될 때 원인이 점수인지 움직임인지는 밖에서 볼 방법이 없어서
 * 디버그 오버레이가 그대로 띄운다.
 */
export type PalmHoldGate =
  | "HOLDING" // 누적 중
  | "CONFIRMED" // 이 프레임에 발사
  | "LOW_SCORE" // Open_Palm 점수가 문턱 미달
  | "MOVING" // 손이 너무 빨라 "정지" 조건 실패
  | "REFRACTORY" // 확정 직후 재누적 금지 구간
  | "WARMING_UP"; // 비교할 이전 프레임이 없어 판정 불가

export interface PalmHoldOutput {
  /** 0~1 유지 진행률. 차징 바 UI용 */
  progress: number;
  /** 이 프레임에 CONFIRM이 발사되면 true (1회성) */
  confirmed: boolean;
  /** 누적 중인지, 아니면 무엇이 막았는지 */
  gate: PalmHoldGate;
  /** 손목 x 이동 속도(정규화 x/ms). 임계는 PALM_MAX_SPEED */
  speed: number;
}

function idle(gate: PalmHoldGate, speed = 0): PalmHoldOutput {
  return { progress: 0, confirmed: false, gate, speed };
}

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

    if (timestampMs < this.refractoryUntil) return idle("REFRACTORY");
    if (!prev) return idle("WARMING_UP");

    const dt = timestampMs - prev.t;
    if (dt <= 0) {
      // 같은 프레임이 두 번 들어온 경우 — 새 정보가 없으니 누적도 판정도 없다.
      return {
        progress: this.heldMs / PALM_HOLD_DURATION_MS,
        confirmed: false,
        gate: "WARMING_UP",
        speed: 0,
      };
    }

    const speed = Math.abs(x - prev.x) / dt;
    // 점수를 먼저 본다 — 손 모양부터 아니면 속도는 따질 의미가 없다.
    if (openPalmScore < PALM_MIN_SCORE) {
      this.heldMs = 0;
      return idle("LOW_SCORE", speed);
    }
    if (speed > PALM_MAX_SPEED) {
      this.heldMs = 0;
      return idle("MOVING", speed);
    }

    // 스톨 프레임(무거운 렌더링으로 dt가 수백 ms~수 초)의 가산을 상한으로
    // 자른다 — 안 자르면 실제 유지 시간보다 훨씬 일찍 확정이 발사되고,
    // 큰 dt는 속도 검사(Δx/dt)까지 무력화해 움직이는 손도 통과시킨다.
    this.heldMs += Math.min(dt, PALM_MAX_FRAME_CREDIT_MS);
    if (this.heldMs >= PALM_HOLD_DURATION_MS) {
      this.heldMs = 0;
      this.refractoryUntil = timestampMs + PALM_REFRACTORY_MS;
      return { progress: 0, confirmed: true, gate: "CONFIRMED", speed };
    }
    return {
      progress: this.heldMs / PALM_HOLD_DURATION_MS,
      confirmed: false,
      gate: "HOLDING",
      speed,
    };
  }
}
