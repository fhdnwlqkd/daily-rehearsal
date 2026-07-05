import type { GestureAction } from "../../types";
import {
  SWIPE_COOLDOWN_MS,
  SWIPE_MIN_DISTANCE,
  SWIPE_WINDOW_MS,
} from "./constants";

interface Sample {
  x: number;
  t: number;
}

export interface SwipeDetectorOptions {
  /**
   * 셀피 카메라는 화면에 거울상으로 표시되므로 좌우를 뒤집어 해석한다.
   * 기본 true. (비미러 원본 좌표에서 사용자가 자기 오른쪽으로 손을
   * 움직이면 x는 감소한다.)
   */
  mirrored?: boolean;
}

/**
 * 손목 x좌표 궤적으로 좌우 스와이프를 판별한다.
 * 프레임마다 update()를 호출하고, 손이 사라지면 reset()으로 궤적을 끊는다.
 */
export class SwipeDetector {
  private samples: Sample[] = [];
  private lastFiredAt = Number.NEGATIVE_INFINITY;
  private readonly mirrored: boolean;

  constructor(options: SwipeDetectorOptions = {}) {
    this.mirrored = options.mirrored ?? true;
  }

  reset(): void {
    this.samples = [];
  }

  update(
    x: number,
    timestampMs: number,
  ): Extract<GestureAction, "NEXT" | "PREV"> | null {
    this.samples.push({ x, t: timestampMs });
    this.samples = this.samples.filter(
      (sample) => timestampMs - sample.t <= SWIPE_WINDOW_MS,
    );

    if (timestampMs - this.lastFiredAt < SWIPE_COOLDOWN_MS) return null;

    const oldest = this.samples[0];
    if (!oldest) return null;

    const dx = x - oldest.x;
    if (Math.abs(dx) < SWIPE_MIN_DISTANCE) return null;

    this.lastFiredAt = timestampMs;
    this.samples = [];

    const movedRightOnScreen = this.mirrored ? dx < 0 : dx > 0;
    return movedRightOnScreen ? "NEXT" : "PREV";
  }
}
