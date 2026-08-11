import type { GestureAction } from "../../types";
import {
  SWIPE_MIN_DISTANCE,
  SWIPE_REFRACTORY_MS,
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
    // 불응기: 판정만 미루는 게 아니라 궤적 기록 자체를 버린다.
    // 기록을 유지하면 복귀 동작(return stroke)이 윈도우에 쌓였다가
    // 불응기 만료 순간 반대 방향으로 발사된다 — 불응기 이후에 새로
    // 시작한 동작만 다음 스와이프가 될 수 있어야 한다.
    if (timestampMs - this.lastFiredAt < SWIPE_REFRACTORY_MS) {
      if (this.samples.length > 0) this.samples = [];
      return null;
    }

    this.samples.push({ x, t: timestampMs });
    this.samples = this.samples.filter(
      (sample) => timestampMs - sample.t <= SWIPE_WINDOW_MS,
    );

    const oldest = this.samples[0];
    if (!oldest) return null;

    const dx = x - oldest.x;
    if (Math.abs(dx) < SWIPE_MIN_DISTANCE) return null;

    const movedRightOnScreen = this.mirrored ? dx < 0 : dx > 0;
    const action = movedRightOnScreen ? "NEXT" : "PREV";

    this.lastFiredAt = timestampMs;
    this.samples = [];
    return action;
  }
}
