"use client";

import { useEffect, useRef, useState } from "react";
import { GestureRecognizer } from "@mediapipe/tasks-vision";
import {
  PALM_MAX_SPEED,
  PALM_MIN_SCORE,
  SWIPE_MIN_DISTANCE,
} from "../../lib/gesture/constants";
import type { GestureDebugFrame } from "../../lib/gesture/debug-bus";
import { readGestureDebugFrame } from "../../lib/gesture/debug-bus";
import type { SourceRect } from "../../lib/gesture/visible-rect";

/** 이 시간 넘게 갱신이 없으면 인식 루프가 안 도는 것으로 본다 */
const STALE_FRAME_MS = 500;
/** 발사된 액션을 이 시간 동안 크게 표시 */
const ACTION_FLASH_MS = 1200;

const WRIST = 0;
const FINGERTIP = 12;

/**
 * 팜홀드가 왜 안 차오르는지를 한 단어로. 게이트가 손모양·정지 두 개라
 * 진행률만 봐서는 어느 쪽이 막는지 알 수 없다 — 그게 이 줄의 존재 이유다.
 */
const PALM_GATE_COLOR: Record<GestureDebugFrame["palmGate"], string> = {
  HOLDING: "rgba(163, 230, 53, 0.9)", // 라임 — 차오르는 중
  CONFIRMED: "rgba(255, 255, 255, 1)", // 흰색 섬광 — 발사
  LOW_SCORE: "rgba(148, 163, 184, 0.75)", // 회색 — 손바닥이 아님
  MOVING: "rgba(251, 146, 60, 0.9)", // 주황 — 움직여서 리셋
  REFRACTORY: "rgba(56, 189, 248, 0.8)", // 하늘 — 확정 직후 잠금
  WARMING_UP: "rgba(163, 230, 53, 0.6)",
  NO_HAND: "rgba(163, 230, 53, 0.6)",
};

const PALM_GATE_LABEL: Record<GestureDebugFrame["palmGate"], string> = {
  HOLDING: "누적 중",
  CONFIRMED: "확정!",
  LOW_SCORE: "← 손모양이 아님",
  MOVING: "← 손이 움직임",
  REFRACTORY: "불응기(확정 직후)",
  WARMING_UP: "첫 프레임",
  NO_HAND: "손 없음",
};

/**
 * MediaPipe가 지금 무엇을 보고 있는지 화면에 그대로 띄우는 디버그 오버레이.
 * 손 스켈레톤(거울상 보정)과 판정 원재료(제스처 점수·스와이프 이동량·팜홀드
 * 진행률·FPS)를 함께 보여준다 — 임계 상수 튜닝은 이 오버레이의 실측값으로 한다.
 *
 * 기본은 꺼져 있고 **D 키**로 켠다. 켜지 않으면 rAF조차 돌지 않으므로
 * 전시 중에 마운트돼 있어도 비용이 없다. 인식 루프가 프레임을 흘려 넣는
 * 전역 슬롯(debug-bus)에서 읽기만 하고, 값은 캔버스와 ref DOM에만 써서
 * 초당 수십 회 리렌더로 측정 대상(인식 성능)을 흔들지 않는다.
 */
export function GestureDebugOverlay() {
  const [visible, setVisible] = useState(false);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const hudRef = useRef<HTMLPreElement>(null);
  const actionRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.repeat || event.key.toLowerCase() !== "d") return;
      // 브리핑 키보드 fallback 등 텍스트 입력 중의 "d"는 토글이 아니다
      const target = event.target;
      if (
        target instanceof HTMLInputElement ||
        target instanceof HTMLTextAreaElement ||
        (target instanceof HTMLElement && target.isContentEditable)
      )
        return;
      setVisible((shown) => !shown);
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  useEffect(() => {
    if (!visible) return;
    const canvas = canvasRef.current;
    const context = canvas?.getContext("2d");
    if (!canvas || !context) return;

    let rafId = 0;

    function draw() {
      rafId = requestAnimationFrame(draw);
      if (!canvas || !context) return;

      // 뷰포트 변화(회전·리사이즈)를 매 프레임 따라간다 — 배경 video와
      // 같은 박스를 쓰므로 별도 리스너 없이 이것으로 충분하다.
      const ratio = window.devicePixelRatio || 1;
      const width = canvas.clientWidth;
      const height = canvas.clientHeight;
      if (canvas.width !== width * ratio || canvas.height !== height * ratio) {
        canvas.width = width * ratio;
        canvas.height = height * ratio;
      }
      context.setTransform(ratio, 0, 0, ratio, 0, 0);
      context.clearRect(0, 0, width, height);

      const frame = readGestureDebugFrame();
      const stale =
        !frame || performance.now() - frame.timestampMs > STALE_FRAME_MS;

      if (hudRef.current) {
        hudRef.current.textContent = formatHud(frame, stale);
      }
      if (actionRef.current) {
        const fresh =
          frame &&
          frame.lastAction &&
          performance.now() - frame.lastActionAtMs < ACTION_FLASH_MS;
        actionRef.current.textContent = fresh ? frame.lastAction : "";
      }

      if (!frame || stale) return;
      const box = {
        width,
        height,
        videoWidth: frame.videoWidth,
        videoHeight: frame.videoHeight,
        crop: frame.crop,
      };
      // 인식 영역 경계 — 이 선 밖의 사람은 아예 후보가 되지 않는다.
      drawCropBounds(context, box);
      if (frame.landmarks) {
        drawHand(context, frame.landmarks, box, {
          gate: frame.palmGate,
          progress: frame.confirmProgress,
        });
      }
      // MediaPipe가 실제로 받은 픽셀. 여기 안 보이는 사람은 인식될 수 없다.
      drawInputPreview(context, frame.inputCanvas, width, height);
    }

    rafId = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(rafId);
  }, [visible]);

  if (!visible) return null;

  return (
    // z-[120]: 권한 안내(z-100) 등 어떤 레이어보다 위 — 디버그는 항상 보여야 한다
    <div className="pointer-events-none absolute inset-0 z-[120]">
      <canvas ref={canvasRef} className="h-full w-full" />
      <pre
        ref={hudRef}
        className="absolute bottom-4 left-4 m-0 rounded-lg border border-lime-300/30 bg-black/70 px-3 py-2 font-mono text-[11px] leading-[1.5] whitespace-pre text-lime-300"
      />
      <div
        ref={actionRef}
        className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 font-mono text-6xl font-bold tracking-widest text-lime-300/80"
      />
    </div>
  );
}

interface Box {
  width: number;
  height: number;
  videoWidth: number;
  videoHeight: number;
  crop: SourceRect | null;
}

/**
 * 랜드마크를 화면 좌표로 옮긴다. 두 단계다.
 *
 * ① 랜드마크는 **인식에 넘긴 크롭 사각형** 기준으로 정규화돼 있으므로 먼저
 *    카메라 프레임 픽셀 좌표로 되돌린다(크롭을 안 했으면 이 단계는 생략).
 * ② 배경 video가 `object-cover` + `scaleX(-1)`이라 같은 규칙을 재현한다 —
 *    잘려나간 축을 고려한 cover 스케일 + 좌우 반전.
 */
function project(
  landmark: { x: number; y: number },
  { width, height, videoWidth, videoHeight, crop }: Box,
): { x: number; y: number } {
  if (!videoWidth || !videoHeight) {
    return { x: (1 - landmark.x) * width, y: landmark.y * height };
  }

  const framePoint = crop
    ? { x: crop.sx + landmark.x * crop.sw, y: crop.sy + landmark.y * crop.sh }
    : { x: landmark.x * videoWidth, y: landmark.y * videoHeight };

  const scale = Math.max(width / videoWidth, height / videoHeight);
  const offsetX = (width - videoWidth * scale) / 2;
  const offsetY = (height - videoHeight * scale) / 2;
  return {
    x: offsetX + (videoWidth - framePoint.x) * scale,
    y: offsetY + framePoint.y * scale,
  };
}

/** 인식 입력 썸네일의 최대 크기(px)와 화면 가장자리 여백 */
const PREVIEW_MAX = 220;
const PREVIEW_INSET = 16;

/**
 * MediaPipe에 넘긴 캔버스를 우측 하단에 그대로 띄운다.
 *
 * "화면 밖 사람이 인식되지 않는다"는 주장을 추론 없이 판정하는 도구다 —
 * 이 썸네일에 옆사람이 보이면 아직 인식 후보이고, 안 보이면 후보가 될
 * 방법이 없다(모델에 들어간 픽셀이 이게 전부이므로). 배경 영상과 같은
 * 좌우 반전을 걸어 화면과 직접 비교할 수 있게 한다.
 */
function drawInputPreview(
  context: CanvasRenderingContext2D,
  input: HTMLCanvasElement | null,
  width: number,
  height: number,
): void {
  if (!input || input.width <= 0 || input.height <= 0) return;

  const scale = Math.min(PREVIEW_MAX / input.width, PREVIEW_MAX / input.height);
  const drawWidth = input.width * scale;
  const drawHeight = input.height * scale;
  const left = width - drawWidth - PREVIEW_INSET;
  const top = height - drawHeight - PREVIEW_INSET;

  context.save();
  context.translate(left + drawWidth, top);
  context.scale(-1, 1); // 배경 영상과 같은 거울상
  context.drawImage(input, 0, 0, drawWidth, drawHeight);
  context.restore();

  context.save();
  context.lineWidth = 2;
  context.strokeStyle = "rgba(163, 230, 53, 0.7)";
  context.strokeRect(left, top, drawWidth, drawHeight);
  context.fillStyle = "rgba(163, 230, 53, 0.9)";
  context.font = "11px ui-monospace, monospace";
  context.fillText("MediaPipe 입력", left, top - 6);
  context.restore();
}

/**
 * 인식에 넘긴 영역의 경계를 점선으로 그린다. 화면(세로)보다 살짝 넓은
 * 사각형이 나오는 게 정상 — margin만큼의 여유다. 이 선 바깥에 서 있는
 * 사람은 MediaPipe에 아예 들어가지 않는다.
 */
function drawCropBounds(context: CanvasRenderingContext2D, box: Box): void {
  if (!box.crop) return;
  const topLeft = project({ x: 0, y: 0 }, box);
  const bottomRight = project({ x: 1, y: 1 }, box);
  const left = Math.min(topLeft.x, bottomRight.x);
  const top = Math.min(topLeft.y, bottomRight.y);

  context.save();
  context.setLineDash([10, 8]);
  context.lineWidth = 2;
  context.strokeStyle = "rgba(163, 230, 53, 0.45)";
  context.strokeRect(
    left,
    top,
    Math.abs(bottomRight.x - topLeft.x),
    Math.abs(bottomRight.y - topLeft.y),
  );
  context.restore();
}

interface PalmState {
  gate: GestureDebugFrame["palmGate"];
  progress: number;
}

function drawHand(
  context: CanvasRenderingContext2D,
  landmarks: { x: number; y: number }[],
  box: Box,
  palm: PalmState,
): void {
  const points = landmarks.map((landmark) => project(landmark, box));

  // 스켈레톤 색 = 팜홀드 게이트 상태. 손을 보고 있는 채로 "지금 왜 안
  // 차오르는지"를 읽을 수 있어야 구석 HUD를 안 쳐다본다.
  context.lineWidth = 3;
  context.strokeStyle = PALM_GATE_COLOR[palm.gate];
  context.beginPath();
  for (const { start, end } of GestureRecognizer.HAND_CONNECTIONS) {
    const from = points[start];
    const to = points[end];
    if (!from || !to) continue;
    context.moveTo(from.x, from.y);
    context.lineTo(to.x, to.y);
  }
  context.stroke();

  points.forEach((point, index) => {
    // 판정에 실제로 쓰이는 두 점만 크게·다른 색으로 — 스와이프는 손끝(12),
    // 팜홀드 정지 판정은 손목(0)을 본다.
    const tracked = index === WRIST || index === FINGERTIP;
    context.fillStyle = tracked
      ? index === FINGERTIP
        ? "rgba(244, 114, 182, 0.95)"
        : "rgba(250, 204, 21, 0.95)"
      : "rgba(255, 255, 255, 0.8)";
    context.beginPath();
    context.arc(point.x, point.y, tracked ? 8 : 4, 0, Math.PI * 2);
    context.fill();
  });

  const wrist = points[WRIST];
  if (wrist) drawHoldRing(context, wrist, palm);
}

/** 손목을 감싸는 차징 링 — HUD의 확정 바와 같은 값을 손 위에 올린다 */
function drawHoldRing(
  context: CanvasRenderingContext2D,
  wrist: { x: number; y: number },
  palm: PalmState,
): void {
  const radius = 34;
  context.save();
  context.lineWidth = 6;
  context.lineCap = "round";

  context.strokeStyle = "rgba(255, 255, 255, 0.18)";
  context.beginPath();
  context.arc(wrist.x, wrist.y, radius, 0, Math.PI * 2);
  context.stroke();

  const swept = palm.gate === "CONFIRMED" ? 1 : palm.progress;
  if (swept > 0) {
    context.strokeStyle = PALM_GATE_COLOR[palm.gate];
    context.beginPath();
    // 12시에서 시계방향 — 차오르는 방향이 직관적이다
    context.arc(
      wrist.x,
      wrist.y,
      radius,
      -Math.PI / 2,
      -Math.PI / 2 + swept * Math.PI * 2,
    );
    context.stroke();
  }
  context.restore();
}

function formatHud(
  frame: ReturnType<typeof readGestureDebugFrame>,
  stale: boolean,
): string {
  if (!frame || stale) {
    return [
      "MEDIAPIPE DEBUG (D로 끄기)",
      "loop  : 정지 — 제스처를 쓰는 스테이지가 아니거나 카메라가 없음",
    ].join("\n");
  }

  const swipeRatio = Math.abs(frame.swipeDx) / SWIPE_MIN_DISTANCE;
  const direction = frame.swipeDx === 0 ? "" : frame.swipeDx < 0 ? "→" : "←";

  return [
    "MEDIAPIPE DEBUG (D로 끄기)",
    `hand  : ${frame.landmarks ? "보임" : "없음"}   fps ${frame.fps.toFixed(0)}   cam ${frame.videoWidth}×${frame.videoHeight}`,
    `인식역: ${
      frame.crop
        ? `${Math.round(frame.crop.sw)}×${Math.round(frame.crop.sh)} (프레임 폭의 ${((frame.crop.sw / frame.videoWidth) * 100).toFixed(0)}%)`
        : "전체 — 크롭 안 됨"
    }`,
    `제스처: ${frame.gestureName ?? "—"} ${frame.gestureScore.toFixed(2)}`,
    `손모양: palm ${frame.openPalmScore.toFixed(2)} / ${PALM_MIN_SCORE} ${pass(frame.openPalmScore >= PALM_MIN_SCORE)} ${bar(frame.openPalmScore / PALM_MIN_SCORE)}`,
    `정지  : ${frame.palmSpeed.toFixed(5)} / ${PALM_MAX_SPEED} ${pass(frame.palmSpeed <= PALM_MAX_SPEED)} ${bar(frame.palmSpeed / PALM_MAX_SPEED)}`,
    `확정  : ${(frame.confirmProgress * 100).toFixed(0)}%  ${bar(frame.confirmProgress)}  ${PALM_GATE_LABEL[frame.palmGate]}`,
    `스와이프: dx ${frame.swipeDx.toFixed(3)} / ${SWIPE_MIN_DISTANCE} ${direction} ${bar(swipeRatio)}`,
    `마지막 : ${frame.lastAction ?? "—"}`,
  ].join("\n");
}

/** 게이트 통과 여부를 한 글자로 */
function pass(ok: boolean): string {
  return ok ? "OK" : "--";
}

/** 0~1(초과 허용) 비율을 10칸 막대로 — 임계까지 얼마나 남았는지 눈으로 본다 */
function bar(ratio: number): string {
  const filled = Math.max(0, Math.min(10, Math.round(ratio * 10)));
  return `${"█".repeat(filled)}${"·".repeat(10 - filled)}`;
}
