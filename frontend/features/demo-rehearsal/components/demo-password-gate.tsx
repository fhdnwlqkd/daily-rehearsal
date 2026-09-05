"use client";

import { useState, type SyntheticEvent } from "react";
import { LockKeyhole } from "lucide-react";
import { useRouter } from "next/navigation";

export function DemoPasswordGate({ configured }: { configured: boolean }) {
  const router = useRouter();
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState(
    configured ? "" : "서버의 데모 인증 환경변수를 먼저 설정해주세요.",
  );
  const [submitting, setSubmitting] = useState(false);

  function submit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!configured || submitting) return;
    void authenticate();
  }

  async function authenticate() {
    setSubmitting(true);
    setMessage("");
    try {
      const response = await fetch("/api/demo/auth", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ password }),
      });
      const body = (await response.json().catch(() => null)) as {
        message?: string;
      } | null;

      if (!response.ok) {
        setMessage(body?.message ?? "데모 인증에 실패했습니다.");
        return;
      }

      router.refresh();
    } catch {
      setMessage("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex h-dvh w-full items-center justify-center bg-[#091017] px-6 text-white">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_35%,rgba(0,176,240,0.15),transparent_45%)]" />
      <form
        onSubmit={submit}
        className="relative w-full max-w-md rounded-[28px] border border-white/15 bg-white/[0.07] px-8 py-10 shadow-2xl backdrop-blur-xl"
      >
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10">
          <LockKeyhole className="h-5 w-5 text-[#58c8ef]" />
        </div>
        <p className="mt-8 text-xs tracking-[0.3em] text-white/45">
          DAILY REHEARSAL · DEMO
        </p>
        <h1 className="mt-3 text-3xl font-extralight tracking-wide">
          발표 데모 입장
        </h1>
        <p className="mt-3 font-light text-white/55">
          발표 담당자에게 전달받은 비밀번호를 입력해주세요.
        </p>

        <label
          className="mt-8 block text-sm text-white/70"
          htmlFor="demo-password"
        >
          비밀번호
        </label>
        <input
          id="demo-password"
          type="password"
          autoComplete="current-password"
          autoFocus
          disabled={!configured || submitting}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          className="mt-2 w-full rounded-2xl border border-white/15 bg-black/25 px-4 py-3.5 transition outline-none focus:border-[#58c8ef]/70 disabled:opacity-50"
        />

        {message && (
          <p className="mt-3 text-sm leading-relaxed text-amber-200/85">
            {message}
          </p>
        )}

        <button
          type="submit"
          disabled={!configured || submitting || password.length === 0}
          className="mt-6 w-full cursor-pointer rounded-2xl bg-white px-5 py-3.5 font-semibold text-[#091017] transition hover:bg-[#dff6ff] disabled:cursor-not-allowed disabled:opacity-40"
        >
          {submitting ? "확인하는 중…" : "데모 시작"}
        </button>
      </form>
    </main>
  );
}
