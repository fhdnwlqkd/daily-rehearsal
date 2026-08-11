import { describe, expect, it } from "vitest";

import { SWIPE_REFRACTORY_MS } from "./constants";
import { SwipeDetector } from "./swipe-detector";

/**
 * fromMs~toMs 동안 x를 fromX→toX로 선형 이동시키며 프레임을 공급하는 헬퍼.
 * 발사가 일어나면 그 시점에 멈추고 결과를 돌려준다.
 */
function sweep(
  detector: SwipeDetector,
  {
    fromX,
    toX,
    fromMs,
    toMs,
    stepMs = 33,
  }: {
    fromX: number;
    toX: number;
    fromMs: number;
    toMs: number;
    stepMs?: number;
  },
) {
  let action: ReturnType<SwipeDetector["update"]> = null;
  for (let t = fromMs; t <= toMs; t += stepMs) {
    const progress = (t - fromMs) / (toMs - fromMs);
    const x = fromX + (toX - fromX) * progress;
    action = detector.update(x, t);
    if (action) return { action, atMs: t };
  }
  return { action, atMs: toMs };
}

describe("SwipeDetector", () => {
  // mirrored 기본값(true) 기준: 원본 좌표에서 x 감소 = 화면상 오른쪽 = NEXT

  it("윈도우 안에서 임계 거리를 이동하면 발사된다", () => {
    const detector = new SwipeDetector();
    const result = sweep(detector, {
      fromX: 0.8,
      toX: 0.4,
      fromMs: 0,
      toMs: 250,
    });
    expect(result.action).toBe("NEXT");
  });

  it("반대 방향 이동은 PREV로 발사된다", () => {
    const detector = new SwipeDetector();
    const result = sweep(detector, {
      fromX: 0.4,
      toX: 0.8,
      fromMs: 0,
      toMs: 250,
    });
    expect(result.action).toBe("PREV");
  });

  it("임계 거리 미만의 이동은 무시된다", () => {
    const detector = new SwipeDetector();
    const result = sweep(detector, {
      fromX: 0.5,
      toX: 0.58,
      fromMs: 0,
      toMs: 250,
    });
    expect(result.action).toBeNull();
  });

  it("윈도우보다 느린 이동은 거리가 커도 발사되지 않는다", () => {
    const detector = new SwipeDetector();
    // 총 0.4를 3초에 걸쳐 이동 — 어느 윈도우 구간에서도 임계 미달
    const result = sweep(detector, {
      fromX: 0.8,
      toX: 0.4,
      fromMs: 0,
      toMs: 3000,
    });
    expect(result.action).toBeNull();
  });

  it("불응기 중의 복귀 동작은 만료 직후 반대 방향으로 발사되지 않는다", () => {
    const detector = new SwipeDetector();
    const fired = sweep(detector, {
      fromX: 0.8,
      toX: 0.4,
      fromMs: 0,
      toMs: 250,
    });
    expect(fired.action).toBe("NEXT");

    // 손을 천천히 제자리로 — 불응기 만료 시점을 걸쳐서 이동한다.
    // 구 구현은 만료 전 궤적이 살아남아 만료 직후 PREV가 발사됐다.
    const returnStroke = sweep(detector, {
      fromX: 0.4,
      toX: 0.8,
      fromMs: fired.atMs + 33,
      toMs: fired.atMs + SWIPE_REFRACTORY_MS + 400,
    });
    expect(returnStroke.action).toBeNull();
  });

  it("불응기 중에는 같은 방향의 완전한 재스와이프도 무시된다", () => {
    const detector = new SwipeDetector();
    const fired = sweep(detector, {
      fromX: 0.8,
      toX: 0.4,
      fromMs: 0,
      toMs: 250,
    });
    expect(fired.action).toBe("NEXT");

    const during = sweep(detector, {
      fromX: 0.8,
      toX: 0.4,
      fromMs: fired.atMs + 100,
      toMs: fired.atMs + 400,
    });
    expect(during.action).toBeNull();
  });

  it("불응기가 끝난 뒤 새로 시작한 스와이프는 정상 발사된다", () => {
    const detector = new SwipeDetector();
    const fired = sweep(detector, {
      fromX: 0.8,
      toX: 0.4,
      fromMs: 0,
      toMs: 250,
    });
    expect(fired.action).toBe("NEXT");

    const after = sweep(detector, {
      fromX: 0.8,
      toX: 0.4,
      fromMs: fired.atMs + SWIPE_REFRACTORY_MS + 50,
      toMs: fired.atMs + SWIPE_REFRACTORY_MS + 300,
    });
    expect(after.action).toBe("NEXT");
  });

  it("reset()은 궤적을 끊는다 — 이전 이동과 이어붙지 않는다", () => {
    const detector = new SwipeDetector();
    sweep(detector, { fromX: 0.8, toX: 0.72, fromMs: 0, toMs: 100 });
    detector.reset();
    // reset 없이 이어졌다면 총 0.8→0.62로 임계를 넘었을 이동
    const result = sweep(detector, {
      fromX: 0.72,
      toX: 0.62,
      fromMs: 133,
      toMs: 233,
    });
    expect(result.action).toBeNull();
  });
});
