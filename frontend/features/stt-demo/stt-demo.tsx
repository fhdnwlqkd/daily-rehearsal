"use client";

import { useCallback, useEffect, useState } from "react";
import {
  STT_MAX_FAILS_BEFORE_FALLBACK,
  useSpeechToText,
} from "@/features/smart-mirror";

/**
 * STT 훅(#31) 검증 데모. 머지 전 제거 여부는 그때 결정.
 *
 * 검증 항목:
 * - 발화 → 실시간 transcript → 침묵 시 CANDIDATE → 확정/다시 말하기
 * - Chrome이 세션을 혼자 끝내도 transcript가 이어지는지(auto-restart)
 * - 네트워크 차단 시 ERROR/NETWORK → retry → N회 실패 시 키보드 fallback
 * - 확정 시 밖으로 나가는 건 transcript 문자열 하나뿐인지(제출 로그)
 */
export function SttDemo() {
  const [submitted, setSubmitted] = useState<string[]>([]);
  const [log, setLog] = useState<string[]>([]);

  const appendLog = useCallback((line: string) => {
    setLog((prev) =>
      [`${new Date().toLocaleTimeString()} ${line}`, ...prev].slice(0, 12),
    );
  }, []);

  const handleConfirm = useCallback(
    (transcript: string) => {
      setSubmitted((prev) => [transcript, ...prev]);
      appendLog(`확정 제출: "${transcript}"`);
    },
    [appendLog],
  );

  const stt = useSpeechToText({ onConfirm: handleConfirm });
  const {
    status,
    transcript,
    errorType,
    failCount,
    start,
    confirm,
    cancel,
    retry,
  } = stt;

  // 상태 전이 로그
  useEffect(() => {
    appendLog(`status=${status}${errorType ? ` (${errorType})` : ""}`);
  }, [status, errorType, appendLog]);

  // 비복구 에러 또는 반복 실패 → 키보드 fallback
  const showKeyboardFallback =
    errorType === "UNSUPPORTED" ||
    errorType === "PERMISSION" ||
    failCount >= STT_MAX_FAILS_BEFORE_FALLBACK;

  // 실제 스테이지에서는 제스처(팜홀드)에 묶일 자리 — 데모는 키보드로 대신한다
  useEffect(() => {
    function handleKey(event: KeyboardEvent) {
      if (event.repeat) return;
      if (event.target instanceof HTMLInputElement) return; // fallback 입력 중 무시
      if (event.code === "Space") start();
      if (event.code === "Enter") confirm();
      if (event.code === "Escape") cancel();
      if (event.code === "KeyR") retry();
    }
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [start, confirm, cancel, retry]);

  return (
    <main className="flex min-h-screen flex-col items-center gap-8 bg-black p-10 text-white">
      <h1 className="text-2xl">STT Hook 검증 데모</h1>

      {/* 상태 패널 */}
      <section className="flex gap-6 rounded border border-white/20 p-4 text-sm">
        <span>status: {status}</span>
        <span>error: {errorType ?? "—"}</span>
        <span>failCount: {failCount}</span>
      </section>

      {/* 조작 */}
      <section className="flex gap-3 text-sm">
        <DemoButton onClick={start} label="듣기 시작 (Space)" />
        <DemoButton onClick={confirm} label="확정 (Enter)" />
        <DemoButton onClick={cancel} label="다시 말하기 (Esc)" />
        <DemoButton onClick={retry} label="재시도 (R)" />
      </section>

      {/* 실시간/후보 transcript */}
      <section className="w-full max-w-2xl rounded-xl border border-white/20 p-6">
        <p className="mb-2 text-xs tracking-widest text-white/50">
          {status === "CANDIDATE"
            ? "후보 — Enter로 확정, Esc로 다시 말하기"
            : "실시간 transcript"}
        </p>
        <p className="min-h-8 text-xl">
          {transcript || (
            <span className="text-white/30">
              {status === "LISTENING" ? "말해보세요…" : "대기 중"}
            </span>
          )}
        </p>
      </section>

      {/* 키보드 fallback */}
      {showKeyboardFallback && (
        <KeyboardFallback
          onSubmit={(text) => {
            handleConfirm(text);
            cancel(); // STT 쪽 상태도 정리
          }}
        />
      )}

      {/* 제출 로그: 백엔드로 나갈 문자열이 이것뿐임을 확인하는 용도 */}
      <section className="w-full max-w-2xl text-sm">
        <p className="mb-1 text-white/50">제출된 transcript</p>
        {submitted.length === 0 ? (
          <p className="text-white/30">아직 없음</p>
        ) : (
          submitted.map((text, index) => (
            <p key={`${text}-${index}`} className="text-emerald-300">
              → {text}
            </p>
          ))
        )}
      </section>

      {/* 이벤트 로그 */}
      <section className="w-full max-w-2xl text-xs text-white/50">
        {log.map((line, index) => (
          <p key={`${line}-${index}`}>{line}</p>
        ))}
      </section>
    </main>
  );
}

function DemoButton({
  onClick,
  label,
}: {
  onClick: () => void;
  label: string;
}) {
  return (
    <button
      className="rounded border border-white/40 px-3 py-1 hover:bg-white/10"
      onClick={onClick}
    >
      {label}
    </button>
  );
}

function KeyboardFallback({ onSubmit }: { onSubmit: (text: string) => void }) {
  const [value, setValue] = useState("");

  return (
    <section className="w-full max-w-2xl rounded-xl border border-amber-400/40 p-4">
      <p className="mb-2 text-sm text-amber-300">
        음성 인식을 사용할 수 없어요 — 키보드로 입력해 주세요
      </p>
      <form
        className="flex gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          const trimmed = value.trim();
          if (!trimmed) return;
          onSubmit(trimmed);
          setValue("");
        }}
      >
        <input
          className="flex-1 rounded border border-white/30 bg-transparent px-3 py-2"
          value={value}
          onChange={(event) => setValue(event.target.value)}
          placeholder="말하려던 내용을 입력…"
        />
        <button className="rounded border border-white/40 px-3" type="submit">
          제출
        </button>
      </form>
    </section>
  );
}
