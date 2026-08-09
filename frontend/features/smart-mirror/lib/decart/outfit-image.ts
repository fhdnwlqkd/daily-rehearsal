/**
 * 옷 참조/썸네일 이미지는 CloudFront에서 오는데 CORS 헤더가 없다 (2026-08-06
 * 실측). Decart SDK의 setImage는 URL 문자열을 브라우저 fetch로 읽으므로
 * 교차 출처 그대로는 실패한다 — 같은 출처의 이미지 프록시
 * (app/api/outfit-image/route.ts)를 거친 URL로 바꿔서 넘긴다.
 */

/** 프록시가 중계를 허용하는 원본 호스트 접미사. SSRF 방지용 허용 목록. */
export const ALLOWED_IMAGE_HOST_SUFFIX = ".cloudfront.net";

const PROXY_PATH = "/api/outfit-image";

/**
 * 원본 이미지 URL을 같은 출처의 프록시 경유 URL로 바꾼다.
 * 허용되지 않는 URL(비 https, 허용 목록 밖 호스트)은 null — 호출부는
 * 해당 옷의 프리뷰 적용을 건너뛴다(전시 안 멈춤 원칙).
 */
export function toProxiedOutfitImageUrl(sourceUrl: string): string | null {
  if (!isAllowedImageUrl(sourceUrl)) return null;
  return `${PROXY_PATH}?src=${encodeURIComponent(sourceUrl)}`;
}

/** 프록시 라우트(서버)와 클라이언트가 같은 판정을 쓰도록 공유하는 검증. */
export function isAllowedImageUrl(sourceUrl: string): boolean {
  let url: URL;
  try {
    url = new URL(sourceUrl);
  } catch {
    return false;
  }
  return url.protocol === "https:" && isAllowedImageHost(url.hostname);
}

function isAllowedImageHost(hostname: string): boolean {
  // "evil.com?.cloudfront.net" 같은 위장을 막기 위해 접미사가 아니라
  // 도메인 경계(레이블 단위)로 판정한다.
  return (
    hostname.endsWith(ALLOWED_IMAGE_HOST_SUFFIX) &&
    hostname.length > ALLOWED_IMAGE_HOST_SUFFIX.length
  );
}
