export { SmartMirror } from "./smart-mirror";
export { TicketPreview } from "./components/stages/ticket-stage";
export { useCamera } from "./hooks/use-camera";
export { useGestureEngine } from "./hooks/use-gesture-engine";
export { useGestureController } from "./hooks/use-gesture-controller";
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
