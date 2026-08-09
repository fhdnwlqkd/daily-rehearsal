import { describe, expect, it } from "vitest";

import { PALM_HOLD_DURATION_MS, PALM_MAX_FRAME_CREDIT_MS } from "./constants";
import { PalmHoldDetector } from "./palm-hold-detector";

/** 정지한 손바닥 프레임을 일정 간격으로 공급하는 헬퍼. */
function feed(
  detector: PalmHoldDetector,
  { fromMs, toMs, stepMs }: { fromMs: number; toMs: number; stepMs: number },
) {
  let last = { progress: 0, confirmed: false };
  for (let t = fromMs; t <= toMs; t += stepMs) {
    last = detector.update({ openPalmScore: 1, x: 0.5, timestampMs: t });
    if (last.confirmed) return { ...last, atMs: t };
  }
  return { ...last, atMs: toMs };
}

describe("PalmHoldDetector", () => {
  it("정상 프레임(33ms)으로 유지 시간을 채우면 확정이 발사된다", () => {
    const detector = new PalmHoldDetector();
    const result = feed(detector, {
      fromMs: 0,
      toMs: PALM_HOLD_DURATION_MS * 2,
      stepMs: 33,
    });
    expect(result.confirmed).toBe(true);
    // 유지 시간 근처(±프레임 몇 개)에서 발사되어야 한다
    expect(result.atMs).toBeGreaterThanOrEqual(PALM_HOLD_DURATION_MS);
    expect(result.atMs).toBeLessThan(PALM_HOLD_DURATION_MS + 200);
  });

  it("스톨 프레임(dt 1200ms)은 상한만큼만 가산된다 — 조기 확정 방지", () => {
    const detector = new PalmHoldDetector();
    // 300ms 정상 유지 (바 20% 지점)
    feed(detector, { fromMs: 0, toMs: 300, stepMs: 33 });
    // 메인 스레드 1200ms 스톨 후 한 프레임 — 무제한 가산이면 여기서
    // 300+1200 >= 1500 이라 확정이 터진다 (실테스트에서 관측된 버그).
    const afterStall = detector.update({
      openPalmScore: 1,
      x: 0.5,
      timestampMs: 300 + 1200,
    });
    expect(afterStall.confirmed).toBe(false);
    // 가산은 상한까지만: 300 + PALM_MAX_FRAME_CREDIT_MS
    expect(afterStall.progress).toBeCloseTo(
      (300 + PALM_MAX_FRAME_CREDIT_MS) / PALM_HOLD_DURATION_MS,
      2,
    );
  });

  it("스톨을 겪어도 계속 유지하면 결국 확정된다", () => {
    const detector = new PalmHoldDetector();
    feed(detector, { fromMs: 0, toMs: 300, stepMs: 33 });
    detector.update({ openPalmScore: 1, x: 0.5, timestampMs: 1500 }); // 스톨
    const result = feed(detector, {
      fromMs: 1533,
      toMs: 1500 + PALM_HOLD_DURATION_MS * 2,
      stepMs: 33,
    });
    expect(result.confirmed).toBe(true);
  });

  it("손 모양이 풀리면 누적이 리셋된다", () => {
    const detector = new PalmHoldDetector();
    feed(detector, { fromMs: 0, toMs: 900, stepMs: 33 });
    detector.update({ openPalmScore: 0, x: 0.5, timestampMs: 933 }); // 풀림
    const resumed = detector.update({
      openPalmScore: 1,
      x: 0.5,
      timestampMs: 966,
    });
    expect(resumed.confirmed).toBe(false);
    expect(resumed.progress).toBeLessThan(0.1);
  });
});
