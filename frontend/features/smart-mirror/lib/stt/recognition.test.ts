import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { SttSnapshot } from "../../types";
import { SILENCE_CONFIRM_MS, STT_LANG } from "./constants";
import type {
  SpeechRecognitionLike,
  SttErrorEventLike,
  SttResultEventLike,
} from "./recognition";
import { SttController } from "./recognition";

/** 테스트에서 이벤트를 수동으로 쏘는 SpeechRecognition 대역 */
class FakeRecognition implements SpeechRecognitionLike {
  lang = "";
  continuous = false;
  interimResults = false;
  onresult: ((event: SttResultEventLike) => void) | null = null;
  onerror: ((event: SttErrorEventLike) => void) | null = null;
  onend: (() => void) | null = null;

  startCalls = 0;
  stopCalls = 0;
  abortCalls = 0;

  start(): void {
    this.startCalls += 1;
  }

  stop(): void {
    this.stopCalls += 1;
  }

  abort(): void {
    this.abortCalls += 1;
  }

  emitResult(segments: Array<{ text: string; isFinal: boolean }>): void {
    const results = segments.map((segment) => ({
      isFinal: segment.isFinal,
      0: { transcript: segment.text },
      length: 1,
    }));
    this.onresult?.({ resultIndex: 0, results });
  }

  emitError(error: string): void {
    this.onerror?.({ error });
  }

  emitEnd(): void {
    this.onend?.();
  }
}

function setup(options: { supported?: boolean } = {}) {
  const supported = options.supported ?? true;
  const instances: FakeRecognition[] = [];
  const snapshots: SttSnapshot[] = [];

  const controller = new SttController({
    createRecognition: () => {
      if (!supported) return null;
      const fake = new FakeRecognition();
      instances.push(fake);
      return fake;
    },
    onChange: (snapshot) => {
      snapshots.push(snapshot);
    },
  });

  const latest = (): SttSnapshot => {
    const snapshot = snapshots[snapshots.length - 1];
    if (!snapshot) throw new Error("아직 통지된 snapshot이 없다");
    return snapshot;
  };
  const current = (): FakeRecognition => {
    const fake = instances[instances.length - 1];
    if (!fake) throw new Error("아직 생성된 recognition이 없다");
    return fake;
  };

  return { controller, instances, snapshots, latest, current };
}

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
});

describe("SttController 시작", () => {
  it("start()로 IDLE에서 LISTENING으로 전이하고 recognition을 구동한다", () => {
    const { controller, latest, current } = setup();

    controller.start();

    expect(latest().status).toBe("LISTENING");
    expect(current().startCalls).toBe(1);
    expect(current().lang).toBe(STT_LANG);
    expect(current().continuous).toBe(true);
    expect(current().interimResults).toBe(true);
  });

  it("브라우저 미지원이면 ERROR/UNSUPPORTED가 된다", () => {
    const { controller, latest } = setup({ supported: false });

    controller.start();

    expect(latest().status).toBe("ERROR");
    expect(latest().errorType).toBe("UNSUPPORTED");
  });

  it("LISTENING 중 start()를 다시 불러도 무시한다", () => {
    const { controller, instances } = setup();

    controller.start();
    controller.start();

    expect(instances).toHaveLength(1);
  });
});

describe("transcript 누적", () => {
  it("interim 결과를 실시간으로 반영하고 final로 교체한다", () => {
    const { controller, latest, current } = setup();
    controller.start();

    current().emitResult([{ text: "내일 아침에", isFinal: false }]);
    expect(latest().transcript).toBe("내일 아침에");

    current().emitResult([{ text: "내일 아침에 발표가 있어", isFinal: true }]);
    expect(latest().transcript).toBe("내일 아침에 발표가 있어");
  });

  it("final 뒤에 오는 interim은 이어붙인다", () => {
    const { controller, latest, current } = setup();
    controller.start();

    current().emitResult([
      { text: "내일 발표가 있어", isFinal: true },
      { text: "조금 긴장돼", isFinal: false },
    ]);

    expect(latest().transcript).toBe("내일 발표가 있어 조금 긴장돼");
  });
});

describe("침묵 감지 → CANDIDATE", () => {
  it("결과 이후 침묵이 지나면 CANDIDATE로 전이하고 recognition을 멈춘다", () => {
    const { controller, latest, current } = setup();
    controller.start();

    current().emitResult([{ text: "내일 발표가 있어", isFinal: true }]);
    vi.advanceTimersByTime(SILENCE_CONFIRM_MS);

    expect(latest().status).toBe("CANDIDATE");
    expect(current().stopCalls).toBe(1);
  });

  it("결과가 이어지는 동안에는 침묵 타이머가 리셋된다", () => {
    const { controller, latest, current } = setup();
    controller.start();

    current().emitResult([{ text: "내일", isFinal: false }]);
    vi.advanceTimersByTime(SILENCE_CONFIRM_MS - 100);
    current().emitResult([{ text: "내일 발표가", isFinal: false }]);
    vi.advanceTimersByTime(SILENCE_CONFIRM_MS - 100);

    expect(latest().status).toBe("LISTENING");
  });

  it("transcript가 비어 있으면 침묵이 지나도 LISTENING을 유지한다", () => {
    const { controller, latest, current } = setup();
    controller.start();

    current().emitResult([{ text: "  ", isFinal: false }]);
    vi.advanceTimersByTime(SILENCE_CONFIRM_MS);

    expect(latest().status).toBe("LISTENING");
    expect(current().stopCalls).toBe(0);
  });

  it("stop() 직후 늦게 도착한 final 결과도 후보 transcript에 포함한다", () => {
    const { controller, latest, current } = setup();
    controller.start();

    current().emitResult([{ text: "내일 발표가", isFinal: false }]);
    vi.advanceTimersByTime(SILENCE_CONFIRM_MS);
    expect(latest().status).toBe("CANDIDATE");

    // Chrome은 stop() 후 pending final을 flush한 뒤 onend를 낸다
    current().emitResult([{ text: "내일 발표가 있어", isFinal: true }]);
    current().emitEnd();

    expect(latest().status).toBe("CANDIDATE");
    expect(latest().transcript).toBe("내일 발표가 있어");
  });
});

describe("auto-restart", () => {
  it("LISTENING 중 예기치 않게 끝나면 새 recognition으로 재시작한다", () => {
    const { controller, latest, instances, current } = setup();
    controller.start();

    current().emitResult([{ text: "내일 발표가 있어", isFinal: true }]);
    current().emitEnd();

    expect(instances).toHaveLength(2);
    expect(current().startCalls).toBe(1);
    expect(latest().status).toBe("LISTENING");
    // 이전 세션의 final은 재시작 후에도 유지된다
    expect(latest().transcript).toBe("내일 발표가 있어");
  });

  it("재시작 이후 새 결과는 이전 final 뒤에 이어붙는다", () => {
    const { controller, latest, current } = setup();
    controller.start();

    current().emitResult([{ text: "내일 발표가 있어", isFinal: true }]);
    current().emitEnd();
    current().emitResult([{ text: "조금 긴장돼", isFinal: false }]);

    expect(latest().transcript).toBe("내일 발표가 있어 조금 긴장돼");
  });

  it("no-speech 에러는 ERROR가 아니며 이어지는 onend에서 재시작한다", () => {
    const { controller, latest, instances, current } = setup();
    controller.start();

    current().emitError("no-speech");
    expect(latest().status).toBe("LISTENING");

    current().emitEnd();
    expect(instances).toHaveLength(2);
    expect(latest().status).toBe("LISTENING");
  });
});

describe("confirm / cancel", () => {
  function toCandidate(ctx: ReturnType<typeof setup>) {
    ctx.controller.start();
    ctx.current().emitResult([{ text: "내일 발표가 있어", isFinal: true }]);
    vi.advanceTimersByTime(SILENCE_CONFIRM_MS);
    ctx.current().emitEnd();
  }

  it("CANDIDATE에서 confirm()하면 transcript를 돌려주고 IDLE로 초기화한다", () => {
    const ctx = setup();
    toCandidate(ctx);

    const confirmed = ctx.controller.confirm();

    expect(confirmed).toBe("내일 발표가 있어");
    expect(ctx.latest().status).toBe("IDLE");
    expect(ctx.latest().transcript).toBe("");
    expect(ctx.latest().failCount).toBe(0);
  });

  it("CANDIDATE가 아니면 confirm()은 null을 돌려주고 아무것도 바꾸지 않는다", () => {
    const ctx = setup();
    ctx.controller.start();

    expect(ctx.controller.confirm()).toBeNull();
    expect(ctx.latest().status).toBe("LISTENING");
  });

  it("CANDIDATE에서 cancel()하면 transcript를 폐기하고 IDLE로 돌아간다", () => {
    const ctx = setup();
    toCandidate(ctx);

    ctx.controller.cancel();

    expect(ctx.latest().status).toBe("IDLE");
    expect(ctx.latest().transcript).toBe("");
  });

  it("LISTENING 중 cancel()하면 abort하고 IDLE로 돌아가며 재시작하지 않는다", () => {
    const ctx = setup();
    ctx.controller.start();
    ctx.current().emitResult([{ text: "내일", isFinal: false }]);

    ctx.controller.cancel();
    expect(ctx.latest().status).toBe("IDLE");
    expect(ctx.current().abortCalls).toBe(1);

    // abort 이후 도착하는 onend가 재시작을 일으키면 안 된다
    ctx.current().emitEnd();
    expect(ctx.instances).toHaveLength(1);
  });
});

describe("에러 처리", () => {
  it("network 에러는 ERROR/NETWORK로 전이하고 failCount를 올린다", () => {
    const { controller, latest, current } = setup();
    controller.start();

    current().emitError("network");
    current().emitEnd();

    expect(latest().status).toBe("ERROR");
    expect(latest().errorType).toBe("NETWORK");
    expect(latest().failCount).toBe(1);
  });

  it("권한 거부는 ERROR/PERMISSION으로 전이한다", () => {
    const { controller, latest, current } = setup();
    controller.start();

    current().emitError("not-allowed");

    expect(latest().status).toBe("ERROR");
    expect(latest().errorType).toBe("PERMISSION");
  });

  it("ERROR에서 retry()하면 failCount를 유지한 채 다시 LISTENING이 된다", () => {
    const { controller, latest, instances, current } = setup();
    controller.start();
    current().emitError("network");
    current().emitEnd();

    controller.retry();

    expect(instances).toHaveLength(2);
    expect(latest().status).toBe("LISTENING");
    expect(latest().failCount).toBe(1);

    current().emitError("network");
    expect(latest().failCount).toBe(2);
  });

  it("ERROR 중에는 침묵 타이머가 CANDIDATE를 만들지 않는다", () => {
    const { controller, latest, current } = setup();
    controller.start();
    current().emitResult([{ text: "내일", isFinal: false }]);

    current().emitError("network");
    vi.advanceTimersByTime(SILENCE_CONFIRM_MS);

    expect(latest().status).toBe("ERROR");
  });
});

describe("dispose", () => {
  it("dispose()하면 recognition을 중단하고 이후 이벤트를 무시한다", () => {
    const { controller, instances, current, snapshots } = setup();
    controller.start();

    controller.dispose();
    expect(current().abortCalls).toBe(1);

    const countBefore = snapshots.length;
    current().emitEnd();

    expect(instances).toHaveLength(1);
    expect(snapshots.length).toBe(countBefore);
  });
});
