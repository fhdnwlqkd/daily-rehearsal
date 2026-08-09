import type { GestureAction } from "../../types";
import {
  SWIPE_COOLDOWN_MS,
  SWIPE_MIN_DISTANCE,
  SWIPE_OPPOSITE_COOLDOWN_MS,
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
  private lastFiredAction: Extract<GestureAction, "NEXT" | "PREV"> | null =
    null;
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

    const movedRightOnScreen = this.mirrored ? dx < 0 : dx > 0;
    const action = movedRightOnScreen ? "NEXT" : "PREV";

    // return stroke 억제: 스와이프한 손이 제자리로 돌아오는 동작이
    // 반대 방향 스와이프로 오인식되지 않게, 직전 발사의 반대 방향은
    // 더 긴 불응기를 적용한다.
    if (
      this.lastFiredAction !== null &&
      action !== this.lastFiredAction &&
      timestampMs - this.lastFiredAt < SWIPE_OPPOSITE_COOLDOWN_MS
    ) {
      return null;
    }

    this.lastFiredAt = timestampMs;
    this.lastFiredAction = action;
    this.samples = [];
    return action;
  }
}
