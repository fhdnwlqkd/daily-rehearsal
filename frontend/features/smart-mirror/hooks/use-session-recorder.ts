"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import {
  DEFAULT_AV_SYNC_DELAY_MS,
  RECORDER_TIMESLICE_MS,
} from "../lib/recording/constants";
import { pickRecordingMimeType } from "../lib/recording/mime";
import { decideRecordingSource } from "../lib/recording/source";
import type { DecartConnectionStatus } from "../types";

/**
 * IDLE       녹화 전 (구간 밖이거나 소스 대기 중)
 * RECORDING  녹화 중 — REC 표시의 근거
 * STOPPED    녹화 종결 (세션 리마운트 전까지 재시작 없음 — 한 세션 한 테이크)
 */
export type SessionRecorderStatus = "IDLE" | "RECORDING" | "STOPPED";

interface UseSessionRecorderArgs {
  /** 옷 입히기~시뮬레이션 구간만 true. false로 내려가면 녹화를 끝낸다. */
  enabled: boolean;
  /** 소스 결정용 — CONNECTING 동안은 기다리고, ERROR/CLOSED면 원본으로 폴백한다. */
  decartStatus: DecartConnectionStatus;
  /** Decart 변환 스트림 — 있으면 이것을 녹화한다("내일의 모습"이 콘텐츠다). */
  decartStream: MediaStream | null;
  /** 카메라 원본 — 마이크 오디오 공급원이자 Decart 불가 시의 영상 폴백. */
  cameraStream: MediaStream | null;
  /** Decart g2g 실측(ms). 녹화 중 도착하면 DelayNode가 그 값을 따라잡는다. */
  syncDelayMs: number | null;
}

interface UseSessionRecorderResult {
  status: SessionRecorderStatus;
}

/**
 * 세션 녹화의 단일 소유자 (세션 층 전용 — 이슈 #94).
 *
 * 스테이지가 아니라 세션 층이 소유하는 이유: 녹화는 옷 입히기 진입부터
 * 시뮬레이션 종료까지 스테이지 전환(언마운트)을 가로질러 이어진다 —
 * Decart 연결과 같은 소유권 논리다.
 *
 * A/V 싱크 (#90 PoC 검증): 비디오는 Decart 왕복+AI 추론만큼 늦게 도착해
 * 소리가 앞서간다 → 마이크를 Web Audio DelayNode로 같은 만큼 지연시킨다.
 * 실측(g2gMs) 도착 전에는 PoC 실측 기반 기본값으로 시작하고, 도착하면
 * delayTime을 그 값으로 갱신한다 (원본 폴백 녹화는 지연 0).
 *
 * 업로드는 아직 연결하지 않는다 — 종료 시 결과 blob 크기만 로그로 남기고
 * 폐기한다. 업로드 연결(POST /video)은 #94 잔여분으로 티켓 스테이지와
 * 함께 붙인다.
 *
 * 한 세션 한 테이크: 시작 후 소스가 바뀌어도(TTL로 Decart가 끊기는 등)
 * 갈아타지 않는다 — MediaRecorder는 소스 교체가 불가능하고, 끊김 시
 * 원본 거울 폴백은 공식 동작이라 프레임 정지도 기록의 일부로 둔다.
 */
export function useSessionRecorder({
  enabled,
  decartStatus,
  decartStream,
  cameraStream,
  syncDelayMs,
}: UseSessionRecorderArgs): UseSessionRecorderResult {
  const [status, setStatus] = useState<SessionRecorderStatus>("IDLE");

  const recorderRef = useRef<MediaRecorder | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const delayNodeRef = useRef<DelayNode | null>(null);
  const delayedTrackRef = useRef<MediaStreamTrack | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  // 시작 시도는 세션당 한 번 — 미지원 환경에서 매 렌더 재시도하지 않는다.
  const startedRef = useRef(false);
  const recordsDecartRef = useRef(false);

  const cleanupAudio = useCallback(() => {
    delayedTrackRef.current?.stop();
    delayedTrackRef.current = null;
    delayNodeRef.current = null;
    void audioContextRef.current?.close().catch(() => undefined);
    audioContextRef.current = null;
  }, []);

  const stopRecording = useCallback(() => {
    const recorder = recorderRef.current;
    recorderRef.current = null;
    if (recorder && recorder.state !== "inactive") {
      recorder.stop(); // 정리는 onstop에서 — 마지막 청크까지 받은 뒤 닫는다
    } else {
      cleanupAudio();
    }
  }, [cleanupAudio]);

  // 녹화 시작 — 구간 안에서 소스가 준비되는 순간 한 번.
  useEffect(() => {
    if (!enabled || startedRef.current) return;
    if (typeof MediaRecorder === "undefined") return;

    // 소스 결정은 lib/recording/source.ts — 실패·종료가 확정됐을 때만 원본
    // 폴백이고, 그 전(진입 직후의 IDLE 포함)에는 다음 상태 변화까지 기다린다.
    const decartTrack = decartStream?.getVideoTracks()[0];
    const decision = decideRecordingSource(
      decartStatus,
      decartTrack !== undefined,
    );
    if (decision === "WAIT") return;
    const videoTrack =
      decision === "DECART" ? decartTrack : cameraStream?.getVideoTracks()[0];
    if (!videoTrack) return;

    const mimeType = pickRecordingMimeType((candidate) =>
      MediaRecorder.isTypeSupported(candidate),
    );
    if (!mimeType) {
      console.warn("지원되는 녹화 mimeType이 없어 세션 녹화를 건너뜁니다");
      startedRef.current = true;
      return;
    }

    startedRef.current = true;
    recordsDecartRef.current = decision === "DECART";

    const recordingTracks: MediaStreamTrack[] = [videoTrack];

    // 마이크가 있으면 싱크 보정을 거쳐 태운다 — 없으면 영상만 녹화한다.
    const micTrack = cameraStream?.getAudioTracks()[0];
    if (micTrack) {
      const audioContext = new AudioContext();
      // 자동재생 정책상 페이지에 클릭·키 입력이 한 번도 없으면 suspended로
      // 시작한다 — 전시 부스는 제스처·음성만 쓸 수 있어 그대로 두면 녹음이
      // 무음이 된다. 캡처 그래프이므로 즉시 재개를 시도하고, 막히면 로그만
      // 남긴다(녹화 실패로 전시를 멈추지 않는다).
      if (audioContext.state === "suspended") {
        audioContext.resume().catch(() => {
          console.warn("녹음 AudioContext resume 실패 — 영상이 무음일 수 있다");
        });
      }
      const source = audioContext.createMediaStreamSource(
        new MediaStream([micTrack]),
      );
      const delayNode = audioContext.createDelay(5);
      const initialDelayMs =
        decision === "DECART" ? (syncDelayMs ?? DEFAULT_AV_SYNC_DELAY_MS) : 0;
      delayNode.delayTime.value = initialDelayMs / 1000;
      const destination = audioContext.createMediaStreamDestination();
      source.connect(delayNode);
      delayNode.connect(destination);

      const delayedTrack = destination.stream.getAudioTracks()[0];
      if (delayedTrack) {
        audioContextRef.current = audioContext;
        delayNodeRef.current = delayNode;
        delayedTrackRef.current = delayedTrack;
        recordingTracks.push(delayedTrack);
      } else {
        void audioContext.close().catch(() => undefined);
      }
    }

    try {
      const recorder = new MediaRecorder(new MediaStream(recordingTracks), {
        mimeType,
      });
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) chunksRef.current.push(event.data);
      };
      recorder.onstop = () => {
        // 업로드 미연결(#94 잔여) — 결과는 크기만 확인하고 폐기한다.
        const blob = new Blob(chunksRef.current, { type: mimeType });
        console.info(
          `세션 녹화 종료 — ${(blob.size / 1024 / 1024).toFixed(2)}MB (${mimeType}) · 업로드 미연결로 폐기`,
        );
        chunksRef.current = [];
        cleanupAudio();
        setStatus("STOPPED");
      };
      recorder.onerror = (event) => {
        console.error("세션 녹화 recorder 에러:", event);
      };
      recorder.start(RECORDER_TIMESLICE_MS);
      recorderRef.current = recorder;
      setStatus("RECORDING");
    } catch (error) {
      // 녹화 실패는 체험을 막지 않는다 — 전시는 그대로 진행된다.
      console.error("세션 녹화를 시작하지 못했습니다:", error);
      cleanupAudio();
    }
  }, [
    enabled,
    decartStatus,
    decartStream,
    cameraStream,
    syncDelayMs,
    cleanupAudio,
  ]);

  // g2g 실측이 녹화 시작 후에 도착하면 지연값을 따라잡는다 (원본 폴백은 0 유지).
  useEffect(() => {
    const delayNode = delayNodeRef.current;
    if (!delayNode || syncDelayMs == null || !recordsDecartRef.current) return;
    delayNode.delayTime.value = syncDelayMs / 1000;
  }, [syncDelayMs]);

  // 구간 이탈(시뮬레이션 종료·세션 리셋) 시 녹화를 끝낸다.
  useEffect(() => {
    if (enabled) return;
    stopRecording();
  }, [enabled, stopRecording]);

  // 언마운트(세션 층 리마운트 포함) 안전망.
  useEffect(() => () => stopRecording(), [stopRecording]);

  return { status };
}
