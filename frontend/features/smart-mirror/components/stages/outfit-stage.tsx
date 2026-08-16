"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { getOutfitSpec } from "../../apis";
import { useConfirmOutfit } from "../../hooks/use-confirm-outfit";
import { useCountdown } from "../../hooks/use-countdown";
import { useGestureController } from "../../hooks/use-gesture-controller";
import { useGetOutfits } from "../../hooks/use-get-outfits";
import { OUTFIT_SELECTION_DURATION_SECONDS } from "../../lib/timing/constants";
import { GestureHint } from "../shared/gesture-hint";
import { GlassPanel } from "../shared/glass-panel";
import { StageCountdown } from "../shared/stage-countdown";
import { StatusLine } from "../shared/status-line";
import type {
  DecartConnectionHandle,
  DecartSpec,
  GestureActionEvent,
  GestureEngineHandle,
  OutfitCandidate,
} from "../../types";

interface OutfitStageProps {
  sessionId: string;
  /** 부스 수명 장비 — 스테이지는 소비만 한다. */
  engine: GestureEngineHandle;
  stream: MediaStream | null;
  /** Decart 연결 핸들(세션 층 소유) — 스테이지는 옷 적용만 요청한다. */
  decart: DecartConnectionHandle;
  /** 옷 확정(PATCH 성공) 시 세션 층으로 올린다 — 시뮬레이션으로 전환된다. */
  onComplete: () => void;
}

/**
 * 3. 옷 입히기 — 내일의 모습을 입어보고 제스처로 고른다 (이슈 #69/#70).
 * 거울 영상 자체(DecartMirrorLayer)가 주인공이라 이 스테이지는 하단
 * 컨트롤(후보 레일·안내)과 상단 타이틀만 그린다. 스와이프 = 즉시 입어보기
 * (spec 조회→적용, 세션 상태 무변화), 팜홀드 = 확정(PATCH→REHEARSAL_READY).
 * 녹화 시작·REC 표시는 여기가 아니라 #94에서 붙는다.
 */
export function OutfitStage({
  sessionId,
  engine,
  stream,
  decart,
  onComplete,
}: OutfitStageProps) {
  const { outfits, status: listStatus } = useGetOutfits(sessionId);
  const { status: confirmStatus, confirm } = useConfirmOutfit(sessionId);
  const [highlightIndex, setHighlightIndex] = useState(0);
  const [selectionExpired, setSelectionExpired] = useState(false);
  // 하이라이트된 옷의 스펙 — 착장 캡션(설명 문구)이 소비한다.
  const [highlightedSpec, setHighlightedSpec] = useState<DecartSpec | null>(
    null,
  );
  // 스펙은 정적 설정이라 스와이프 왕복 시 재조회하지 않는다.
  const specCacheRef = useRef(new Map<string, DecartSpec>());
  const confirmRequestedRef = useRef(false);
  const autoConfirmAttemptedRef = useRef(false);

  const handleSelectionExpired = useCallback(() => {
    setSelectionExpired(true);
  }, []);
  const remainingSeconds = useCountdown({
    durationSeconds: OUTFIT_SELECTION_DURATION_SECONDS,
    onExpire: handleSelectionExpired,
  });

  // 진입 시 기본 옷(defaultOutfit)부터 입혀 보인다 — 목록의 첫 옷이 아닐 수 있다.
  useEffect(() => {
    if (listStatus !== "READY") return;
    const defaultIndex = outfits.findIndex((outfit) => outfit.defaultOutfit);
    if (defaultIndex > 0) setHighlightIndex(defaultIndex);
  }, [listStatus, outfits]);

  const highlighted: OutfitCandidate | null = outfits[highlightIndex] ?? null;
  const highlightedId = highlighted?.outfitId ?? null;
  const { applyOutfit } = decart;

  // 하이라이트가 바뀔 때마다 스펙을 조회해 프리뷰에 적용한다. 연결 전이면
  // applyOutfit이 큐에 쌓아뒀다가 연결 완료 시 반영한다(마지막 것이 이김).
  useEffect(() => {
    if (!highlightedId) return;

    let cancelled = false;

    async function applyHighlighted(outfitId: string) {
      try {
        const cached = specCacheRef.current.get(outfitId);
        const spec = cached ?? (await getOutfitSpec(sessionId, outfitId));
        specCacheRef.current.set(outfitId, spec);
        if (cancelled) return;
        setHighlightedSpec(spec);
        applyOutfit(spec);
      } catch (error) {
        // 스펙 조회 실패 = 이 옷만 못 입혀보는 것 — 스와이프 진행은 막지 않는다.
        console.error("Failed to load outfit spec:", error);
        if (!cancelled) setHighlightedSpec(null);
      }
    }

    void applyHighlighted(highlightedId);

    return () => {
      cancelled = true;
    };
  }, [highlightedId, sessionId, applyOutfit]);

  const confirmHighlighted = useCallback(
    (ignorePreviewGate = false) => {
      // 프리뷰 게이트(2026-08-08 결정): 변환 연결 중에는 확정을 받지 않는다 —
      // 입혀본 모습을 보기도 전에 스테이지를 지나치는 것을 막는다. 그 외
      // 상태(CONNECTED는 물론, 연결 실패 ERROR·종료 CLOSED·카메라 없음 IDLE)는
      // 허용해 전시가 여기서 멈추지 않게 한다.
      if (!ignorePreviewGate && decart.status === "CONNECTING") return;
      // LOADING(중복 요청)·READY(이미 확정) 중에는 막는다. ERROR는 재시도 허용.
      if (
        !highlighted ||
        confirmRequestedRef.current ||
        confirmStatus === "LOADING" ||
        confirmStatus === "READY"
      )
        return;
      confirmRequestedRef.current = true;
      confirm(highlighted.outfitId);
    },
    [decart.status, highlighted, confirmStatus, confirm],
  );

  useEffect(() => {
    if (confirmStatus === "ERROR") confirmRequestedRef.current = false;
  }, [confirmStatus]);

  // 제한시간이 끝난 순간의 하이라이트를 잠그고 자동 확정한다. 목록 로딩이
  // 늦었다면 첫 후보가 준비되는 즉시 한 번만 확정한다.
  useEffect(() => {
    if (!selectionExpired || !highlighted || autoConfirmAttemptedRef.current)
      return;
    autoConfirmAttemptedRef.current = true;
    confirmHighlighted(true);
  }, [selectionExpired, highlighted, confirmHighlighted]);

  const handleAction = useCallback(
    (event: GestureActionEvent) => {
      if (listStatus !== "READY") return;
      if (selectionExpired) {
        if (event.action === "CONFIRM" && confirmStatus === "ERROR") {
          confirmHighlighted(true);
        }
        return;
      }

      if (event.action === "NEXT") {
        setHighlightIndex((index) => Math.min(outfits.length - 1, index + 1));
      }
      if (event.action === "PREV") {
        setHighlightIndex((index) => Math.max(0, index - 1));
      }
      if (event.action === "CONFIRM") {
        confirmHighlighted();
      }
    },
    [
      listStatus,
      selectionExpired,
      confirmStatus,
      outfits.length,
      confirmHighlighted,
    ],
  );

  const {
    status: gestureStatus,
    handVisible,
    confirmProgress,
  } = useGestureController({
    engine,
    stream,
    onAction: handleAction,
    // 확정 성공 후에는 인식 루프·키 입력을 정지해 이중 확정을 막는다.
    enabled:
      confirmStatus !== "READY" &&
      (!selectionExpired || confirmStatus === "ERROR"),
  });

  // 확정 성공 → 세션 층으로 올린다 (시뮬레이션 스테이지로 전환).
  useEffect(() => {
    if (confirmStatus === "READY") onComplete();
  }, [confirmStatus, onComplete]);

  return (
    // pb-20: StageFrame의 TapHint(absolute bottom-6)가 이 아래 깔리므로
    // 하단 안내 슬롯이 그 위에서 끝나도록 여백을 확보한다 (겹침 방지).
    <div className="flex h-full flex-col items-center px-8 pt-24 pb-20">
      <StageCountdown label="옷 선택까지" remainingSeconds={remainingSeconds} />
      <div className="text-center drop-shadow-[0_2px_16px_rgba(0,0,0,0.85)]">
        <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/65">
          TOMORROW&apos;S LOOK
        </p>
        <h2 className="text-3xl font-extralight tracking-wide md:text-4xl">
          내일의 모습을 입어보세요
        </h2>
      </div>

      {/* 중앙은 비워둔다 — 거울(변환 프리뷰)이 곧 화면의 주인공이다 */}
      <div className="flex flex-1 items-center justify-center">
        {decart.status === "CONNECTING" && (
          <StatusLine text="내일의 모습을 준비하는 중…" />
        )}
        {decart.status === "ERROR" && (
          <StatusLine text="옷 입히기를 사용할 수 없습니다 — 이대로 진행할 수 있어요" />
        )}
      </div>

      {/* 착장 캡션 — 지금 입고 있는 옷의 이름과 설명 */}
      <div className="mb-5 h-14 text-center drop-shadow-[0_2px_12px_rgba(0,0,0,0.8)]">
        {highlighted && (
          <>
            <p className="text-xl font-extralight tracking-[0.06em]">
              {highlighted.label}
            </p>
            {highlightedSpec && (
              <p className="mt-1 text-sm font-light tracking-[0.08em] text-white/60">
                {highlightedSpec.prompt}
              </p>
            )}
          </>
        )}
      </div>

      {listStatus === "LOADING" && <StatusLine text="옷장을 여는 중…" />}
      {listStatus === "ERROR" && (
        <StatusLine text="옷 목록을 불러오지 못했습니다" error />
      )}

      {listStatus === "READY" && (
        <div className="flex flex-wrap justify-center gap-5">
          {outfits.map((outfit, index) => (
            <OutfitCard
              key={outfit.outfitId}
              outfit={outfit}
              order={index + 1}
              highlighted={index === highlightIndex}
              confirmProgress={index === highlightIndex ? confirmProgress : 0}
            />
          ))}
        </div>
      )}

      {/* 안내 슬롯 — 상태가 바뀌어도 높이를 고정해 레일이 출렁이지 않게 한다 */}
      <div className="mt-4 flex h-24 items-center justify-center">
        {confirmStatus === "IDLE" && listStatus === "READY" && (
          <GestureHint
            gestureStatus={gestureStatus}
            handVisible={handVisible}
            confirmProgress={confirmProgress}
            highlightedLabel={highlighted?.label ?? null}
            subject="옷"
          />
        )}
        {confirmStatus === "LOADING" && (
          <StatusLine text="이 모습으로 확정하는 중…" />
        )}
        {confirmStatus === "ERROR" && (
          <StatusLine
            text="확정에 실패했습니다 — 손바닥을 펴거나 Enter로 다시 시도"
            error
          />
        )}
      </div>
    </div>
  );
}

function OutfitCard({
  outfit,
  order,
  highlighted,
  confirmProgress,
}: {
  outfit: OutfitCandidate;
  /** 카드 번호(1부터) — 배열 순서. */
  order: number;
  highlighted: boolean;
  /** 0~1 팜홀드 진행률 — 하이라이트 카드에만 차오른다. */
  confirmProgress: number;
}) {
  const charging = confirmProgress > 0;

  return (
    // 타입 카드와 같은 문법: 비선택 카드를 죽여서 선택을 드러낸다.
    <div
      className={`transition-all duration-300 ${highlighted ? "" : "scale-95 opacity-50"}`}
    >
      <GlassPanel
        className={`px-5 py-4 ${
          highlighted ? "border-white/60 bg-white/20" : "border-white/10"
        }`}
        pulsing={highlighted}
        pulseColor="rgba(255, 255, 255, 0.35)"
      >
        <div className="flex w-36 flex-col items-center gap-2.5 text-center">
          <span className="text-xs font-light tracking-[0.3em] text-white/50">
            {String(order).padStart(2, "0")}
          </span>
          {/* 썸네일은 <img> 표시 전용이라 CORS가 필요 없다 — 원본 URL 직결 */}
          <img
            src={outfit.thumbnailUrl}
            alt={outfit.label}
            className="h-20 w-20 rounded-xl border border-white/15 object-cover"
          />
          <span className="text-lg font-extralight tracking-wide">
            {outfit.label}
          </span>
          {/* 팜홀드 차징 바 — 차오를 때만 트랙과 함께 나타난다 (시프트 없음).
              타입 카드(h-1)보다 두껍게: 카드가 작고 시선이 거울(중앙)에 가 있어
              같은 두께로는 전시 거리에서 인지 불가 (2026-08-08 실테스트). */}
          <div
            className={`h-2 w-full overflow-hidden rounded-full transition-colors ${charging ? "bg-white/15" : "bg-transparent"}`}
          >
            <div
              className="h-full rounded-full bg-white/90 transition-[width] duration-100"
              style={{ width: `${confirmProgress * 100}%` }}
            />
          </div>
        </div>
      </GlassPanel>
    </div>
  );
}
