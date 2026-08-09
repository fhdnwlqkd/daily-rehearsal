import type { SttErrorType, SttSnapshot, SttStatus } from "../../types";
import { SILENCE_CONFIRM_MS, STT_LANG } from "./constants";

// TypeScript lib.dom에는 SpeechRecognition 타입이 없어서(비표준 접두사 API),
// 우리가 쓰는 표면만 구조적 타입으로 선언한다. 테스트 대역도 이 타입을 구현한다.

export interface SttResultAlternativeLike {
  transcript: string;
}

export interface SttResultLike {
  isFinal: boolean;
  0: SttResultAlternativeLike;
  length: number;
}

export interface SttResultEventLike {
  resultIndex: number;
  results: ArrayLike<SttResultLike>;
}

export interface SttErrorEventLike {
  error: string;
}

export interface SpeechRecognitionLike {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onresult: ((event: SttResultEventLike) => void) | null;
  onerror: ((event: SttErrorEventLike) => void) | null;
  onend: (() => void) | null;
  start(): void;
  /** pending final을 flush한 뒤 종료한다 (침묵 확정용) */
  stop(): void;
  /** 결과를 버리고 즉시 종료한다 (취소용) */
  abort(): void;
}

/** 미지원 브라우저면 null을 돌려준다 */
export type RecognitionFactory = () => SpeechRecognitionLike | null;

type SpeechRecognitionCtor = new () => SpeechRecognitionLike;

export function createBrowserRecognitionFactory(): RecognitionFactory {
  return () => {
    if (typeof window === "undefined") return null;
    const scope = window as unknown as {
      SpeechRecognition?: SpeechRecognitionCtor;
      webkitSpeechRecognition?: SpeechRecognitionCtor;
    };
    const Ctor = scope.SpeechRecognition ?? scope.webkitSpeechRecognition;
    return Ctor ? new Ctor() : null;
  };
}

export interface SttControllerOptions {
  createRecognition: RecognitionFactory;
  onChange: (snapshot: SttSnapshot) => void;
  silenceMs?: number;
  lang?: string;
}

/**
 * Web Speech API 위의 상태머신. React와 무관한 순수 로직이라 훅 밖에 둔다.
 *
 * IDLE → start() → LISTENING → (침묵 감지) → CANDIDATE → confirm()/cancel() → IDLE
 *
 * 지저분한 부분을 전부 여기 격리한다:
 * - Chrome은 장시간 침묵/자체 타임아웃으로 continuous 세션을 혼자 끝낸다
 *   → LISTENING 중의 onend는 새 인스턴스로 조용히 재시작하고, 이전 세션의
 *   final은 committedText로 넘겨 transcript를 이어붙인다.
 * - stop() 후에도 pending final이 늦게 도착한다 → CANDIDATE에서도 결과를 받는다.
 * - no-speech는 에러가 아니라 "아직 말 안 함"이다 → 무시하고 재시작에 맡긴다.
 */
export class SttController {
  private readonly createRecognition: RecognitionFactory;
  private readonly onChange: (snapshot: SttSnapshot) => void;
  private readonly silenceMs: number;
  private readonly lang: string;

  private status: SttStatus = "IDLE";
  private errorType: SttErrorType | null = null;
  private failCount = 0;

  /** 재시작을 넘어 살아남은 확정 텍스트 */
  private committedText = "";
  /** 현재 recognition 세션의 final 텍스트 */
  private sessionFinal = "";
  /** 현재 recognition 세션의 interim(미확정) 텍스트 */
  private interim = "";

  private recognition: SpeechRecognitionLike | null = null;
  private silenceTimer: ReturnType<typeof setTimeout> | null = null;
  private disposed = false;

  constructor(options: SttControllerOptions) {
    this.createRecognition = options.createRecognition;
    this.onChange = options.onChange;
    this.silenceMs = options.silenceMs ?? SILENCE_CONFIRM_MS;
    this.lang = options.lang ?? STT_LANG;
  }

  start(): void {
    if (this.disposed) return;
    if (this.status === "LISTENING" || this.status === "CANDIDATE") return;
    this.beginSession();
  }

  /** ERROR에서 다시 듣기 시작한다. failCount는 유지된다. */
  retry(): void {
    if (this.disposed || this.status !== "ERROR") return;
    this.beginSession();
  }

  /** CANDIDATE의 후보 transcript를 확정해 돌려준다. 그 외 상태면 null. */
  confirm(): string | null {
    if (this.disposed || this.status !== "CANDIDATE") return null;

    const confirmed = this.composedTranscript();
    this.detachRecognition();
    this.clearTranscript();
    this.status = "IDLE";
    this.errorType = null;
    this.failCount = 0;
    this.emit();
    return confirmed;
  }

  /** 듣던 내용/후보를 폐기하고 IDLE로 돌아간다. */
  cancel(): void {
    if (this.disposed || this.status === "IDLE") return;

    this.clearSilenceTimer();
    this.recognition?.abort();
    this.detachRecognition();
    this.clearTranscript();
    this.status = "IDLE";
    this.errorType = null;
    this.emit();
  }

  dispose(): void {
    if (this.disposed) return;
    this.disposed = true;
    this.clearSilenceTimer();
    this.recognition?.abort();
    this.detachRecognition();
  }

  // --- 내부 ---

  private beginSession(): void {
    const recognition = this.createRecognition();
    if (!recognition) {
      this.status = "ERROR";
      this.errorType = "UNSUPPORTED";
      this.emit();
      return;
    }

    recognition.lang = this.lang;
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.onresult = (event) => {
      this.handleResult(recognition, event);
    };
    recognition.onerror = (event) => {
      this.handleError(recognition, event);
    };
    recognition.onend = () => {
      this.handleEnd(recognition);
    };

    this.recognition = recognition;
    this.status = "LISTENING";
    this.errorType = null;
    recognition.start();
    this.emit();
  }

  private handleResult(
    source: SpeechRecognitionLike,
    event: SttResultEventLike,
  ): void {
    if (this.disposed || source !== this.recognition) return;
    if (this.status !== "LISTENING" && this.status !== "CANDIDATE") return;

    const finals: string[] = [];
    const interims: string[] = [];
    for (let i = 0; i < event.results.length; i += 1) {
      const result = event.results[i];
      if (!result) continue;
      (result.isFinal ? finals : interims).push(result[0].transcript);
    }
    this.sessionFinal = finals.join(" ");
    this.interim = interims.join(" ");

    // CANDIDATE에서는 stop()이 flush한 늦은 final만 반영하고 타이머는 다시 안 건다
    if (this.status === "LISTENING") {
      this.restartSilenceTimer();
    }
    this.emit();
  }

  private handleEnd(source: SpeechRecognitionLike): void {
    if (this.disposed || source !== this.recognition) return;
    if (this.status !== "LISTENING") return;

    // 우리가 멈춘 게 아닌데 끝났다 → 세션 final을 넘겨 담고 조용히 재시작
    this.committedText = this.composedFinalText();
    this.sessionFinal = "";
    this.interim = "";
    this.beginSession();
  }

  private handleError(
    source: SpeechRecognitionLike,
    event: SttErrorEventLike,
  ): void {
    if (this.disposed || source !== this.recognition) return;
    // no-speech: 아직 말을 안 한 것. aborted: 우리가 멈춘 것. 둘 다 에러 아님.
    if (event.error === "no-speech" || event.error === "aborted") return;

    this.clearSilenceTimer();
    if (
      event.error === "not-allowed" ||
      event.error === "service-not-allowed"
    ) {
      this.errorType = "PERMISSION";
    } else if (event.error === "network") {
      this.errorType = "NETWORK";
      this.failCount += 1;
    } else {
      this.errorType = "UNKNOWN";
      this.failCount += 1;
    }
    this.status = "ERROR";
    this.emit();
  }

  private handleSilence(): void {
    if (this.disposed || this.status !== "LISTENING") return;
    if (this.composedTranscript() === "") return;

    // stop()은 pending final을 flush한 뒤 onend를 낸다. onend는
    // LISTENING이 아니면 무시되므로 재시작이 일어나지 않는다.
    this.status = "CANDIDATE";
    this.recognition?.stop();
    this.emit();
  }

  private restartSilenceTimer(): void {
    this.clearSilenceTimer();
    this.silenceTimer = setTimeout(() => {
      this.handleSilence();
    }, this.silenceMs);
  }

  private clearSilenceTimer(): void {
    if (this.silenceTimer !== null) {
      clearTimeout(this.silenceTimer);
      this.silenceTimer = null;
    }
  }

  private detachRecognition(): void {
    // 참조를 끊으면 이 인스턴스의 늦은 이벤트는 source 비교로 전부 무시된다
    this.recognition = null;
  }

  private clearTranscript(): void {
    this.clearSilenceTimer();
    this.committedText = "";
    this.sessionFinal = "";
    this.interim = "";
  }

  private composedFinalText(): string {
    return [this.committedText, this.sessionFinal]
      .map((part) => part.trim())
      .filter(Boolean)
      .join(" ");
  }

  private composedTranscript(): string {
    return [this.committedText, this.sessionFinal, this.interim]
      .map((part) => part.trim())
      .filter(Boolean)
      .join(" ");
  }

  private emit(): void {
    this.onChange({
      status: this.status,
      transcript: this.composedTranscript(),
      errorType: this.errorType,
      failCount: this.failCount,
    });
  }
}
