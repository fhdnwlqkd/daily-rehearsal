import { describe, expect, it } from "vitest";
import { computeVisibleSourceRect } from "./visible-rect";

describe("computeVisibleSourceRect", () => {
  it("세로 모니터 + 가로 카메라: 가운데 세로 띠만 남긴다", () => {
    // 전시장 실제 조합 — 1920×1080 카메라를 1080×1920 모니터에 object-cover.
    const rect = computeVisibleSourceRect({
      videoWidth: 1920,
      videoHeight: 1080,
      displayWidth: 1080,
      displayHeight: 1920,
    });

    // 세로가 꽉 차고(1080) 가로는 9:16 비율만큼만 보인다.
    expect(rect.sh).toBe(1080);
    expect(rect.sw).toBeCloseTo(1080 * (1080 / 1920), 5);
    // 가운데 정렬 — 좌우가 같은 양씩 잘린다.
    expect(rect.sx).toBeCloseTo((1920 - rect.sw) / 2, 5);
    expect(rect.sy).toBe(0);
    // 프레임의 3분의 1도 안 보인다 = 나머지는 전부 화면 밖 관람객 후보였다.
    expect(rect.sw / 1920).toBeLessThan(0.35);
  });

  it("비율이 같으면 전체 프레임이 그대로 보인다", () => {
    const rect = computeVisibleSourceRect({
      videoWidth: 1088,
      videoHeight: 624,
      displayWidth: 1088,
      displayHeight: 624,
    });

    expect(rect).toEqual({ sx: 0, sy: 0, sw: 1088, sh: 624 });
  });

  it("margin은 보이는 영역을 넓히되 프레임을 넘지 않는다", () => {
    const base = computeVisibleSourceRect({
      videoWidth: 1920,
      videoHeight: 1080,
      displayWidth: 1080,
      displayHeight: 1920,
    });
    const grown = computeVisibleSourceRect({
      videoWidth: 1920,
      videoHeight: 1080,
      displayWidth: 1080,
      displayHeight: 1920,
      margin: 0.2,
    });

    expect(grown.sw).toBeCloseTo(base.sw * 1.2, 5);
    // 세로는 이미 꽉 차 있어서 더 넓어질 수 없다.
    expect(grown.sh).toBe(1080);
    expect(grown.sy).toBe(0);
  });

  it("음수 margin은 보이는 영역보다 더 좁게 자른다", () => {
    const rect = computeVisibleSourceRect({
      videoWidth: 1920,
      videoHeight: 1080,
      displayWidth: 1080,
      displayHeight: 1920,
      margin: -0.3,
    });

    expect(rect.sw).toBeCloseTo(1080 * (1080 / 1920) * 0.7, 5);
    expect(rect.sh).toBeCloseTo(1080 * 0.7, 5);
    // 좁혀도 가운데는 유지된다.
    expect(rect.sx + rect.sw / 2).toBeCloseTo(960, 5);
    expect(rect.sy + rect.sh / 2).toBeCloseTo(540, 5);
  });

  it("메타데이터 전(0)에는 자르지 않는다", () => {
    const rect = computeVisibleSourceRect({
      videoWidth: 0,
      videoHeight: 0,
      displayWidth: 1080,
      displayHeight: 1920,
    });

    expect(rect).toEqual({ sx: 0, sy: 0, sw: 0, sh: 0 });
  });
});
