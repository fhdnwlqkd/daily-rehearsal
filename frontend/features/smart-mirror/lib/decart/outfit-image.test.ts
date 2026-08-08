import { describe, expect, it } from "vitest";

import { isAllowedImageUrl, toProxiedOutfitImageUrl } from "./outfit-image";

describe("isAllowedImageUrl", () => {
  it("허용 목록(cloudfront.net) https URL을 통과시킨다", () => {
    expect(
      isAllowedImageUrl(
        "https://dg0nfh2xt4e4z.cloudfront.net/outfits/suit.png",
      ),
    ).toBe(true);
  });

  it("http(비암호화)는 거부한다", () => {
    expect(
      isAllowedImageUrl("http://dg0nfh2xt4e4z.cloudfront.net/outfits/suit.png"),
    ).toBe(false);
  });

  it("허용 목록 밖 호스트는 거부한다", () => {
    expect(isAllowedImageUrl("https://evil.com/outfits/suit.png")).toBe(false);
  });

  it("접미사 위장 호스트를 거부한다", () => {
    // 도메인 경계 없이 endsWith만 쓰면 뚫리는 케이스들
    expect(isAllowedImageUrl("https://evilcloudfront.net/x.png")).toBe(false);
    expect(isAllowedImageUrl("https://cloudfront.net/x.png")).toBe(false);
  });

  it("URL이 아닌 문자열을 거부한다", () => {
    expect(isAllowedImageUrl("not-a-url")).toBe(false);
    expect(isAllowedImageUrl("")).toBe(false);
  });
});

describe("toProxiedOutfitImageUrl", () => {
  it("허용 URL을 프록시 경유 URL로 바꾼다", () => {
    const source = "https://dg0nfh2xt4e4z.cloudfront.net/outfits/suit.png";
    expect(toProxiedOutfitImageUrl(source)).toBe(
      `/api/outfit-image?src=${encodeURIComponent(source)}`,
    );
  });

  it("거부된 URL은 null을 반환한다 — 호출부가 적용을 건너뛴다", () => {
    expect(toProxiedOutfitImageUrl("https://evil.com/suit.png")).toBeNull();
  });
});
