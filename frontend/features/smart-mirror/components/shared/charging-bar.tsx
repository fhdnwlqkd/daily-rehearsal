"use client";

import { useEffect, useRef, type RefObject } from "react";

interface ChargingBarProps {
  /** 인식 루프가 매 프레임 채우는 진행률(0~1) ref */
  progressRef: RefObject<number>;
  /** 이 바가 차오를 차례인지 (하이라이트된 카드만 true) */
  active: boolean;
  /** 트랙 높이 등 — 기본은 타입 카드용 얇은 바 */
  className?: string;
  /** 차오르는 동안만 입히는 트랙 배경 (단일 클래스여야 한다) */
  trackClass?: string;
  fillClass?: string;
}

/**
 * 팜홀드 차징 바. 진행률을 **React state가 아니라 ref에서 매 프레임 직접**
 * 읽어 DOM에 쓴다.
 *
 * state로 30Hz를 밀면 전시장에서 바가 8~9%쯤에서 멈춘 채로 확정이 발사됐다
 * (2026-09-14 현장 영상: 같은 프레임에 디버그 HUD는 47%, 카드 바는 8%).
 * 값은 같은 틱의 같은 변수였으니 문제는 값이 아니라 React→DOM 경로다 —
 * 손이 잡히는 동안은 랜드마크+제스처 분류가 매 프레임 돌아서 렌더가 밀린다.
 * 디버그 오버레이의 손목 링은 같은 값을 캔버스에 직접 그려서 정확했고,
 * 여기도 같은 방식으로 맞춘다. 덤으로 초당 30번의 스테이지 리렌더가 통째로
 * 사라져 인식 루프에 돌아갈 시간이 늘어난다.
 *
 * CSS transition은 일부러 걸지 않는다. 매 프레임 갱신이라 이미 부드럽고,
 * 100ms 트랜지션은 33ms마다 목표가 바뀌면서 오히려 실제보다 뒤처진다.
 */
export function ChargingBar({
  progressRef,
  active,
  className = "h-1",
  trackClass = "bg-white/10",
  fillClass = "bg-white/80",
}: ChargingBarProps) {
  const trackRef = useRef<HTMLDivElement>(null);
  const fillRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let rafId = 0;
    let lastProgress = -1;

    function tick() {
      rafId = requestAnimationFrame(tick);
      const progress = active ? progressRef.current : 0;
      if (progress === lastProgress) return;
      lastProgress = progress;

      if (fillRef.current) {
        fillRef.current.style.width = `${progress * 100}%`;
      }
      // 평소엔 트랙도 투명해서 구분선처럼 보이지 않는다(레이아웃 시프트 없음).
      trackRef.current?.classList.toggle(trackClass, progress > 0);
    }

    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, [active, progressRef, trackClass]);

  return (
    // 트랙 배경은 toggle로만 붙인다 — bg-transparent를 함께 두면 두 배경
    // 유틸의 우선순위가 같아 생성 순서에 따라 승부가 갈린다(차징이 안 보일 수 있다).
    <div
      ref={trackRef}
      className={`${className} w-full overflow-hidden rounded-full transition-colors`}
    >
      <div
        ref={fillRef}
        className={`h-full rounded-full ${fillClass}`}
        style={{ width: 0 }}
      />
    </div>
  );
}
