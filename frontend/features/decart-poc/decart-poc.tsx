"use client";

import {
  createDecartClient,
  models,
  type ConnectionState,
  type RealTimeClient,
} from "@decartai/sdk";
import { useEffect, useRef, useState } from "react";

// #90 PoC: Decart WebRTC 원격 track을 MediaRecorder로 녹화 가능한지 검증. — 결론: 가능 ✅
//
// 검증된 사항:
// - 원격 video track + 로컬 마이크를 한 MediaStream으로 묶어 mp4로 직접 녹화 (iOS 재생 호환)
// - A/V 싱크: 비디오는 Decart 왕복+AI추론(Δ≈0.6s)만큼 늦게 도착하므로 소리가 앞서감
//   → SDK가 실측해주는 glass-to-glass 지연(g2gMs)을 Web Audio DelayNode에 넣어
//     마이크를 같은 만큼 지연시키면 싱크 일치 (박수 테스트로 검증 완료)
// - Decart 과금은 연결 유지 시간 비례 → 녹화 종료 시 즉시 disconnect

// 코덱을 명시하지 않으면("video/mp4") 크롬이 오디오를 Opus-in-MP4로 녹음하는데,
// 이 조합은 카카오톡·iOS 등 외부 플레이어/변환기가 못 읽어 소리가 사라진다.
// H.264(avc1) + AAC(mp4a.40.2)를 명시해 어디서든 재생되는 표준 mp4로 녹화한다.
const MIME_CANDIDATES = [
  'video/mp4;codecs="avc1.42E01E,mp4a.40.2"',
  "video/mp4",
  "video/webm;codecs=vp8",
  "video/webm",
];

type Recording = { url: string; size: number; mimeType: string };

export function DecartPoc() {
  const apiKey = process.env.NEXT_PUBLIC_DECART_API_KEY;

  const localVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRef = useRef<HTMLVideoElement>(null);
  const localStreamRef = useRef<MediaStream | null>(null);
  const remoteStreamRef = useRef<MediaStream | null>(null);
  const clientRef = useRef<RealTimeClient | null>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);

  const [logs, setLogs] = useState<string[]>([]);
  const [connState, setConnState] = useState<ConnectionState | "idle">("idle");
  const [mimeType, setMimeType] = useState("");
  const [isRecording, setIsRecording] = useState(false);
  const [recording, setRecording] = useState<Recording | null>(null);
  const [prompt, setPrompt] = useState("cyberpunk neon style");
  const [delayMs, setDelayMs] = useState(0); // A/V 싱크 보정값 — g2g 실측이 도착하면 자동 설정

  const log = (msg: string) => {
    const time = new Date().toLocaleTimeString("ko-KR", { hour12: false });
    const ms = String(Date.now() % 1000).padStart(3, "0");
    setLogs((prev) => [...prev, `[${time}.${ms}] ${msg}`]);
  };

  useEffect(() => {
    setMimeType(
      MIME_CANDIDATES.find((m) => MediaRecorder.isTypeSupported(m)) ?? "",
    );
  }, []);

  useEffect(() => {
    return () => {
      clientRef.current?.disconnect();
      localStreamRef.current?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  async function connect() {
    if (!apiKey) return;
    try {
      const model = models.realtime("lucy-2.1");
      log("카메라 요청…");
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: {
          frameRate: model.fps,
          width: model.width,
          height: model.height,
        },
      });
      localStreamRef.current = stream;
      if (localVideoRef.current) localVideoRef.current.srcObject = stream;
      log("카메라 OK. Decart 연결 시작…");

      const client = createDecartClient({ apiKey });
      clientRef.current = await client.realtime.connect(stream, {
        model,
        initialState: { prompt: { text: prompt, enhance: true } },
        debugQuality: true, // g2g(카메라→변환→화면) 지연 실측 — 싱크 보정값의 출처
        onConnectionChange: (state) => {
          setConnState(state);
          log(`연결 상태: ${state}`);
        },
        onConnectionQuality: (report) => {
          if (report.warmingUp) return;
          const { g2gMs, fps } = report.metrics;
          if (g2gMs != null) {
            setDelayMs((prev) => (prev === 0 ? Math.round(g2gMs) : prev));
            log(
              `g2g 지연 실측: ${Math.round(g2gMs)}ms (fps=${String(fps)}) → 싱크 보정값으로 사용`,
            );
          }
        },
        onRemoteStream: (remote) => {
          remoteStreamRef.current = remote;
          if (remoteVideoRef.current) remoteVideoRef.current.srcObject = remote;
          const track = remote.getVideoTracks()[0];
          log(`원격 스트림 도착 (video track: ${track ? "있음" : "없음"})`);
          track?.addEventListener("unmute", () =>
            log("video track UNMUTE — 프레임 흐르기 시작"),
          );
        },
      });
      clientRef.current.on("error", (e) => log(`SDK 에러: ${e.message}`));
    } catch (e) {
      log(`연결 실패: ${e instanceof Error ? e.message : String(e)}`);
    }
  }

  function startRecording() {
    const remoteVideo = remoteStreamRef.current?.getVideoTracks()[0];
    const micAudio = localStreamRef.current?.getAudioTracks()[0];
    if (!remoteVideo || !micAudio || delayMs === 0) return;

    // A/V 싱크 보정: 마이크를 DelayNode로 g2g만큼 지연시켜 비디오와 여정 길이를 맞춘다
    const ctx = new AudioContext();
    audioCtxRef.current = ctx;
    const source = ctx.createMediaStreamSource(new MediaStream([micAudio]));
    const delay = ctx.createDelay(5);
    delay.delayTime.value = delayMs / 1000;
    const destination = ctx.createMediaStreamDestination();
    source.connect(delay);
    delay.connect(destination);
    const delayedMic = destination.stream.getAudioTracks()[0];
    if (!delayedMic) {
      log("싱크 보정 스트림 생성 실패");
      return;
    }

    // 원격 비디오 + 지연된 마이크를 한 스트림으로 묶어 MediaRecorder에 투입
    const target = new MediaStream([remoteVideo, delayedMic]);
    try {
      const recorder = new MediaRecorder(target, { mimeType });
      const chunks: Blob[] = [];
      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunks.push(e.data);
      };
      recorder.onstop = () => {
        const blob = new Blob(chunks, { type: mimeType });
        setRecording({
          url: URL.createObjectURL(blob),
          size: blob.size,
          mimeType,
        });
        log(`녹화 완료: ${(blob.size / 1024 / 1024).toFixed(2)}MB`);
        void audioCtxRef.current?.close();
        audioCtxRef.current = null;
        teardownDecart();
      };
      recorder.onerror = (e) => log(`recorder 에러: ${e.type}`);
      recorder.start(1000);
      recorderRef.current = recorder;
      setIsRecording(true);
      setRecording(null);
      log(`녹화 시작 (${mimeType}, 마이크 +${delayMs}ms 싱크 보정)`);
    } catch (e) {
      log(
        `MediaRecorder 생성 실패: ${e instanceof Error ? e.message : String(e)}`,
      );
    }
  }

  function stopRecording() {
    recorderRef.current?.stop();
    setIsRecording(false);
  }

  // Decart 리얼타임은 연결 유지 시간만큼 과금되므로, 녹화가 끝나면 즉시 연결을 끊는다
  function teardownDecart() {
    clientRef.current?.disconnect();
    clientRef.current = null;
    localStreamRef.current?.getTracks().forEach((t) => t.stop());
    localStreamRef.current = null;
    remoteStreamRef.current = null;
    if (localVideoRef.current) localVideoRef.current.srcObject = null;
    if (remoteVideoRef.current) remoteVideoRef.current.srcObject = null;
    setConnState("disconnected");
    setDelayMs(0);
    log("Decart 연결 해제 (과금 중단) — 다시 녹화하려면 ① 연결부터");
  }

  async function applyPrompt() {
    if (!clientRef.current) return;
    await clientRef.current.setPrompt(prompt, { enhance: true });
    log(`프롬프트 변경: "${prompt}"`);
  }

  if (!apiKey) {
    return (
      <main className="p-8 font-mono text-red-600">
        NEXT_PUBLIC_DECART_API_KEY가 없습니다. frontend/.env.local 확인 후 dev
        서버를 재시작하세요.
      </main>
    );
  }

  const connected = connState === "connected" || connState === "generating";
  const readyToRecord = connected && delayMs > 0;

  return (
    <main className="min-h-screen space-y-5 bg-zinc-950 p-6 font-mono text-sm text-zinc-100">
      <h1 className="text-lg font-bold">
        #90 PoC — Decart 원격 track → MediaRecorder 녹화 검증
      </h1>

      <section className="flex flex-wrap items-center gap-3">
        <button
          onClick={() => void connect()}
          disabled={connState !== "idle" && connState !== "disconnected"}
          className="rounded bg-blue-600 px-4 py-2 disabled:opacity-40"
        >
          ① 카메라 + Decart 연결
        </button>
        {!isRecording ? (
          <button
            onClick={startRecording}
            disabled={!readyToRecord}
            className="rounded bg-red-600 px-4 py-2 disabled:opacity-40"
          >
            ② 녹화 시작
          </button>
        ) : (
          <button
            onClick={stopRecording}
            className="animate-pulse rounded bg-red-700 px-4 py-2"
          >
            ③ 녹화 종료 (REC ●)
          </button>
        )}
        <span>
          상태:{" "}
          <b className={connected ? "text-green-400" : "text-yellow-400"}>
            {connState}
          </b>
        </span>
        {connected && !readyToRecord && (
          <span className="text-yellow-400">
            싱크 보정값(g2g) 측정 중… 잠시 후 녹화 가능
          </span>
        )}
        {readyToRecord && !isRecording && (
          <span className="text-green-400">
            보정값 {delayMs}ms 확보 — 녹화 가능
          </span>
        )}
      </section>

      <section>
        <h2 className="mb-1 text-zinc-400">이벤트 로그</h2>
        <pre className="max-h-48 overflow-y-auto rounded bg-zinc-900 p-3 text-xs leading-5">
          {logs.join("\n") || "아직 없음"}
        </pre>
      </section>

      {recording && (
        <section className="space-y-2 rounded border border-green-800 p-4">
          <h2 className="font-bold text-green-400">
            녹화 결과 — {(recording.size / 1024 / 1024).toFixed(2)}MB (
            {recording.mimeType})
          </h2>
          <video
            src={recording.url}
            controls
            className="w-lg max-w-full rounded bg-zinc-900"
          />
          <a
            href={recording.url}
            download={`decart-poc.${recording.mimeType.includes("mp4") ? "mp4" : "webm"}`}
            className="inline-block rounded bg-green-700 px-4 py-2"
          >
            파일 다운로드
          </a>
        </section>
      )}

      <section className="grid grid-cols-2 gap-4">
        <figure>
          <figcaption className="mb-1 text-zinc-400">
            내 카메라 (로컬)
          </figcaption>
          <video
            ref={localVideoRef}
            autoPlay
            playsInline
            muted
            className="w-full rounded bg-zinc-900"
          />
        </figure>
        <figure>
          <figcaption className="mb-1 text-zinc-400">
            Decart 변환 결과 (원격 ← 녹화 대상)
          </figcaption>
          <video
            ref={remoteVideoRef}
            autoPlay
            playsInline
            muted
            className="w-full rounded bg-zinc-900"
          />
        </figure>
      </section>

      <section className="flex flex-wrap items-center gap-3">
        <input
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          className="w-72 rounded border border-zinc-700 bg-zinc-900 px-3 py-2"
          placeholder="변환 프롬프트"
        />
        <button
          onClick={() => void applyPrompt()}
          className="rounded bg-zinc-700 px-4 py-2"
        >
          프롬프트 적용
        </button>
      </section>
    </main>
  );
}
