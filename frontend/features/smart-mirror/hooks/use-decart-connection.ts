"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { createDecartClient, models } from "@decartai/sdk";
import type { RealTimeClient } from "@decartai/sdk";

import { issueDecartToken } from "../apis";
import { mapDecartConnectionState } from "../lib/decart/connection-state";
import { toProxiedOutfitImageUrl } from "../lib/decart/outfit-image";
import { MAX_DECART_CONNECTION_MS } from "../lib/timing/constants";
import type { DecartConnectionHandle, DecartSpec } from "../types";

/**
 * 백엔드 outfit-spec의 model과 같은 값(전시 중 불변). 연결은 스펙 조회보다
 * 먼저 일어나므로 응답을 기다리지 않고 상수로 고정한다.
 */
const DECART_MODEL = models.realtime("lucy-vton-latest");

interface UseDecartConnectionArgs {
  /** 세션 생성 전에는 null — 연결하지 않는다. */
  sessionId: string | null;
  /** 카메라 원본(비디오+마이크). 비디오 트랙만 분리해 보낸다. */
  cameraStream: MediaStream | null;
  /** 옷 입히기~시뮬레이션 구간만 true. false로 내려가면 연결을 해제한다. */
  enabled: boolean;
}

/**
 * Decart WebRTC 연결의 단일 소유자 (세션 층 전용 — 이슈 #69).
 *
 * 스테이지가 아니라 세션 층(ExperienceSession)이 소유하는 이유:
 * 옷 입히기→시뮬레이션 전환에도 변환 프리뷰가 유지되어야 하고(스테이지는
 * 전환 시 언마운트), 이후 녹화(#94)가 같은 출력 스트림을 집어가야 한다.
 *
 * 해제 3지점(과금 방어): ① 언마운트/enabled=false(세션 종료 포함 — 티켓
 * 복귀 시 세션 층 리마운트) ② SDK disconnected ③ MAX_CONNECTION_MS 워치독.
 *
 * 연결 실패는 ERROR로 알리고 끝낸다 — 전시는 멈추지 않고 원본 거울로
 * 진행한다(전시 안 멈춤 원칙). 토큰이 세션당 1회라 자동 재연결은 불가능
 * (백엔드 계약 — TTL·재발급 정책은 백엔드 협의 중).
 */
export function useDecartConnection({
  sessionId,
  cameraStream,
  enabled,
}: UseDecartConnectionArgs): DecartConnectionHandle {
  const [status, setStatus] =
    useState<DecartConnectionHandle["status"]>("IDLE");
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null);
  const [g2gMs, setG2gMs] = useState<number | null>(null);

  const clientRef = useRef<RealTimeClient | null>(null);
  const manuallyClosedRef = useRef(false);
  // 마지막 스펙이 이기는 적용 큐 — 빠른 스와이프 연타로 setImage가 겹치면
  // 중간 옷들은 건너뛰고 최종 하이라이트만 반영한다.
  const applyQueueRef = useRef<{
    running: boolean;
    pending: DecartSpec | null;
  }>({ running: false, pending: null });

  const drainApplyQueue = useCallback(async () => {
    const queue = applyQueueRef.current;
    if (queue.running) return;
    queue.running = true;
    try {
      while (queue.pending) {
        const spec = queue.pending;
        queue.pending = null;

        const client = clientRef.current;
        if (!client) {
          // 연결 전 — 꺼낸 스펙을 큐에 되돌려놔야 연결 완료 시 drain이
          // 다시 집는다 (되돌리지 않으면 기본 옷이 증발 — 08-08 실테스트).
          queue.pending = spec;
          return;
        }

        const proxiedUrl = toProxiedOutfitImageUrl(spec.referenceImageUrl);
        if (!proxiedUrl) {
          console.error("Blocked outfit image URL:", spec.referenceImageUrl);
          continue;
        }

        try {
          // SDK에 URL 문자열을 주지 않는다: 상대 경로는 http(s)로 인식되지
          // 않아 base64 데이터로 오인된다("Invalid set_image image data",
          // 2026-08-06 실테스트). 같은 출처 프록시에서 직접 받아 Blob으로
          // 넘긴다 — tryon-examples(digital-mirror)와 같은 패턴.
          const response = await fetch(proxiedUrl);
          if (!response.ok) {
            throw new Error(`outfit image fetch failed: ${response.status}`);
          }
          const imageBlob = await response.blob();

          await client.setImage(imageBlob, {
            prompt: spec.prompt,
            enhance: spec.enhance,
          });
        } catch (error) {
          // 개별 옷 적용 실패는 프리뷰가 이전 옷에 머무는 것 — 연결 자체를
          // 죽이지 않고 다음 스와이프로 자연 복구되게 둔다.
          console.error("Failed to apply outfit spec:", error);
        }
      }
    } finally {
      queue.running = false;
    }
  }, []);

  const applyOutfit = useCallback(
    (spec: DecartSpec) => {
      if (manuallyClosedRef.current) return;
      applyQueueRef.current.pending = spec;
      void drainApplyQueue();
    },
    [drainApplyQueue],
  );

  const disconnect = useCallback(() => {
    manuallyClosedRef.current = true;
    applyQueueRef.current.pending = null;
    const client = clientRef.current;
    clientRef.current = null;
    client?.disconnect();
    setRemoteStream(null);
    setG2gMs(null);
    setStatus("CLOSED");
  }, []);

  useEffect(() => {
    if (!enabled || !sessionId || !cameraStream) {
      // 연결됐던 세션이 구간을 벗어난 경우(시뮬레이션 종료 등)는 CLOSED,
      // 아직 재료가 없어 연결 전이면 IDLE 유지.
      setStatus((current) => (current === "IDLE" ? "IDLE" : "CLOSED"));
      manuallyClosedRef.current = false;
      return;
    }

    // 가드 통과 시점의 non-null 값을 지역 상수로 고정해 클로저에 넘긴다.
    const targetSessionId = sessionId;
    const sourceStream = cameraStream;

    // 취소 검사를 함수로 감싸는 이유: 지역 변수 직접 검사면 TS가 첫 가드
    // 이후 값을 false로 내로잉해 이후 검사를 "항상 거짓"으로 오판한다
    // (클로저 재할당을 흐름 분석이 못 본다 — no-unnecessary-condition 오탐).
    let cancelled = false;
    const isCancelled = () => cancelled;
    let client: RealTimeClient | null = null;
    let watchdog: ReturnType<typeof setTimeout> | null = null;

    setStatus("CONNECTING");
    setG2gMs(null);

    async function connect() {
      try {
        const { clientToken } = await issueDecartToken(targetSessionId);
        if (isCancelled()) return;

        const decart = createDecartClient({ apiKey: clientToken });
        // 마이크 트랙은 보내지 않는다 — STT는 브라우저에서 따로 돌고,
        // Decart는 영상 변환에만 쓴다(대역폭 절약).
        const videoOnlyStream = new MediaStream(sourceStream.getVideoTracks());

        client = await decart.realtime.connect(videoOnlyStream, {
          model: DECART_MODEL,
          // 서버 기본 720p 대신 큰 거울 화면에 맞는 출력을 받고, 브라우저와
          // MediaRecorder 모두에서 안정적인 VP8 경로를 사용한다.
          resolution: "1080p",
          preferredVideoCodec: "vp8",
          onRemoteStream: (stream) => {
            if (!isCancelled()) setRemoteStream(stream);
          },
          // g2g(카메라→변환→화면) 지연 실측 — 녹화(#94) A/V 싱크 보정값의 출처.
          // 흔들리는 후속 측정으로 오디오가 출렁이지 않게 첫 안정값만 쓴다.
          debugQuality: true,
          onConnectionQuality: (report) => {
            if (isCancelled() || report.warmingUp) return;
            console.info("Decart connection quality:", {
              quality: report.quality,
              limitingFactor: report.limitingFactor,
              ...report.metrics,
            });
            const measured = report.metrics.g2gMs;
            if (measured != null) {
              setG2gMs((current) => current ?? Math.round(measured));
            }
          },
        });

        if (isCancelled()) {
          client.disconnect();
          return;
        }

        client.on("connectionChange", (state) => {
          if (!isCancelled()) setStatus(mapDecartConnectionState(state));
        });
        client.on("error", (error) => {
          if (isCancelled()) return;
          console.error("Decart connection error:", error);
          setStatus("ERROR");
        });

        clientRef.current = client;
        setStatus(mapDecartConnectionState(client.getConnectionState()));

        // 연결 전에 요청된 기본 옷 스펙이 있으면 지금 반영한다.
        void drainApplyQueue();

        watchdog = setTimeout(() => {
          if (isCancelled()) return;
          console.warn("Decart connection watchdog fired — disconnecting");
          disconnect();
        }, MAX_DECART_CONNECTION_MS);
      } catch (error) {
        if (isCancelled()) return;
        // 토큰 발급 실패(재입장 세션의 1회 제한 409 포함)와 WebRTC 수립
        // 실패가 모두 여기로 온다 — 원본 거울로 체험을 계속한다.
        console.error("Failed to connect Decart:", error);
        setStatus("ERROR");
      }
    }

    void connect();

    return () => {
      cancelled = true;
      if (watchdog) clearTimeout(watchdog);
      clientRef.current = null;
      client?.disconnect();
      setRemoteStream(null);
    };
  }, [enabled, sessionId, cameraStream, drainApplyQueue, disconnect]);

  return { status, remoteStream, g2gMs, applyOutfit, disconnect };
}
