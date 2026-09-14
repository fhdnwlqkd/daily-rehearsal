import type { NormalizedLandmark } from "@mediapipe/tasks-vision";
import type { GestureAction } from "../../types";
import type { PalmHoldGate } from "./palm-hold-detector";
import type { SourceRect } from "./visible-rect";

/**
 * 디버그 오버레이가 한 프레임에서 보는 인식 원재료.
 * useGestureController의 rAF 루프가 프레임마다 채워 넣는다.
 */
export interface GestureDebugFrame {
  /** performance.now() 기준 이 프레임의 시각 */
  timestampMs: number;
  /**
   * 감지된 손 한 개의 21개 랜드마크. 손이 없으면 null.
   * 정규화 기준은 카메라 프레임 전체가 아니라 **crop 사각형**이다.
   */
  landmarks: NormalizedLandmark[] | null;
  /** 카메라 프레임 실제 해상도 — 오버레이가 object-cover 좌표를 맞추는 데 쓴다 */
  videoWidth: number;
  videoHeight: number;
  /** 인식에 실제로 넘긴 영역(카메라 픽셀). 자르지 않았으면 null */
  crop: SourceRect | null;
  /**
   * 인식에 넘긴 바로 그 캔버스. 디버그 오버레이가 썸네일로 띄워서
   * "MediaPipe가 보는 화면"을 눈으로 확인하는 데 쓴다 — 크롭이 화면과
   * 어긋났는지, 옆사람이 아직 들어오는지를 추론 없이 판정할 수 있다.
   * 복사하지 않고 참조만 넘기므로 오버레이가 꺼져 있으면 비용이 없다.
   */
  inputCanvas: HTMLCanvasElement | null;
  /** 최고 점수 제스처 분류 이름(없으면 null)과 그 점수 */
  gestureName: string | null;
  gestureScore: number;
  /** 팜홀드 게이트가 실제로 보는 값 */
  openPalmScore: number;
  /**
   * 이번 프레임에 팜홀드 누적을 막은 이유(또는 누적/확정 중).
   * 손 자체가 없으면 "NO_HAND" — 판별기는 그 프레임을 보지도 못한다.
   */
  palmGate: PalmHoldGate | "NO_HAND";
  /** 손목 x 이동 속도(정규화 x/ms). 임계는 PALM_MAX_SPEED */
  palmSpeed: number;
  /** 스와이프 추적점(손끝 landmark 12)의 윈도우 내 누적 이동량 — 임계와 직접 비교 */
  swipeDx: number;
  /** 0~1 팜홀드 진행률 */
  confirmProgress: number;
  /** 인식 루프 실측 FPS */
  fps: number;
  /** 마지막으로 발사된 액션과 그 시각 — 방금 뭐가 나갔는지 확인용 */
  lastAction: GestureAction | null;
  lastActionAtMs: number;
}

/**
 * 최신 프레임 한 장만 들고 있는 모듈 전역 슬롯.
 *
 * 오버레이를 React state로 흘리면 초당 30~60회 리렌더가 나서 측정 대상인
 * 인식 성능 자체를 망친다. 생산자(컨트롤러)는 여기에 덮어쓰기만 하고,
 * 소비자(오버레이)는 자기 rAF에서 읽어 캔버스에만 그린다 — 렌더 트리는
 * 건드리지 않는다. 컨트롤러는 스테이지마다 마운트되지만 동시에 도는 건
 * 언제나 하나라 슬롯 하나로 충분하다.
 */
let latestFrame: GestureDebugFrame | null = null;

export function publishGestureDebugFrame(frame: GestureDebugFrame): void {
  latestFrame = frame;
}

export function readGestureDebugFrame(): GestureDebugFrame | null {
  return latestFrame;
}

/** 인식 루프가 멈출 때(스테이지 전환·언마운트) 남은 잔상을 지운다 */
export function clearGestureDebugFrame(): void {
  latestFrame = null;
}
