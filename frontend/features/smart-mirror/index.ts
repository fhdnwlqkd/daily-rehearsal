export { SmartMirror } from "./smart-mirror";
export { TicketPreview } from "./components/stages/ticket-stage";
export { TicketDownloadPreview } from "./components/ticket/mobile-download-page";
export { useCamera } from "./hooks/use-camera";
export { useGestureEngine } from "./hooks/use-gesture-engine";
export { useGestureController } from "./hooks/use-gesture-controller";
// 팜홀드 차징 바 — 진행률을 ref에서 직접 그린다(데모 기능도 같이 쓴다).
export { ChargingBar } from "./components/shared/charging-bar";
export type {
  UseGestureControllerOptions,
  UseGestureControllerResult,
} from "./hooks/use-gesture-controller";
export { useSpeechToText } from "./hooks/use-speech-to-text";
export type {
  UseSpeechToTextOptions,
  UseSpeechToTextResult,
} from "./hooks/use-speech-to-text";
export { STT_MAX_FAILS_BEFORE_FALLBACK } from "./lib/stt/constants";
export type {
  GestureAction,
  GestureActionEvent,
  GestureEngineHandle,
  GestureEngineStatus,
  SttErrorType,
  SttSnapshot,
  SttStatus,
} from "./types";
// 개발 전용(#232) — 스테이지 단독 프리뷰 (app/dev/stage-preview)
export { StagePreview } from "./components/stage-preview";
// 개발 전용 — MediaPipe 인식 디버그 오버레이(D 키). 데모 기능도 같이 쓴다.
export { GestureDebugOverlay } from "./components/shared/gesture-debug-overlay";
