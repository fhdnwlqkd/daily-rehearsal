/**
 * 행동 지시/상태 안내 한 줄 — 밝은 영상 위에서도 읽히도록 필 스크림을 깐다.
 * 전시 관람 거리(1~2m) 기준 크기. 타입 선택·브리핑이 공유한다 (2026-07-23
 * 크기 통일 — 스테이지별 복제본이 서로 다른 크기가 되는 것을 막는다).
 */
export function StatusLine({
  text,
  error = false,
}: {
  text: string;
  error?: boolean;
}) {
  return (
    <p
      className={`rounded-full bg-black/45 px-6 py-2.5 text-base font-light tracking-[0.2em] backdrop-blur-sm md:text-lg ${error ? "text-red-300/90" : "text-white/90"}`}
    >
      {text}
    </p>
  );
}
