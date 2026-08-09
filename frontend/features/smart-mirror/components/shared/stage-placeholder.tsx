/**
 * 스테이지 개편(#89) 동안 미구현 스테이지가 임시로 띄우는 자리표시자.
 * 모든 스테이지가 실제 화면을 갖추면 이 파일은 삭제한다.
 */
export function StagePlaceholder({ label }: { label: string }) {
  return (
    <div className="flex h-full items-center justify-center">
      <p className="text-sm font-light tracking-[0.3em] text-white/35">
        {label} — 구현 예정
      </p>
    </div>
  );
}
