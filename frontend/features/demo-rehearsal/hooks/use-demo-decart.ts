"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { createDecartClient, models } from "@decartai/sdk";
import type { RealTimeClient } from "@decartai/sdk";
import { issueDemoDecartToken } from "../apis";
import { DEMO_TIMING } from "../data/scenario";
import type { DemoDecartHandle, DemoDecartStatus, DemoOutfit } from "../types";

const DEMO_DECART_MODEL = models.realtime("lucy-vton-latest");

function mapConnectionState(state: string): DemoDecartStatus {
  if (state === "connected" || state === "generating") return "CONNECTED";
  if (state === "connecting" || state === "reconnecting") return "CONNECTING";
  if (state === "disconnected") return "CLOSED";
  return "CONNECTING";
}

export function useDemoDecart({
  cameraStream,
  enabled,
}: {
  cameraStream: MediaStream | null;
  enabled: boolean;
}): DemoDecartHandle {
  const [status, setStatus] = useState<DemoDecartStatus>("IDLE");
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null);
  const clientRef = useRef<RealTimeClient | null>(null);
  const pendingOutfitRef = useRef<DemoOutfit | null>(null);
  const applyingRef = useRef(false);

  const applyPendingOutfit = useCallback(async () => {
    if (applyingRef.current) return;
    applyingRef.current = true;
    try {
      while (pendingOutfitRef.current) {
        const outfit = pendingOutfitRef.current;
        pendingOutfitRef.current = null;
        const client = clientRef.current;
        if (!client) {
          pendingOutfitRef.current = outfit;
          return;
        }

        try {
          const response = await fetch(outfit.imageUrl, {
            cache: "force-cache",
          });
          if (!response.ok) {
            throw new Error(`demo outfit fetch failed: ${response.status}`);
          }
          await client.setImage(await response.blob(), {
            prompt: outfit.prompt,
            enhance: outfit.enhance,
          });
        } catch (error) {
          console.error("Failed to apply demo outfit:", error);
        }
      }
    } finally {
      applyingRef.current = false;
    }
  }, []);

  const applyOutfit = useCallback(
    (outfit: DemoOutfit) => {
      pendingOutfitRef.current = outfit;
      void applyPendingOutfit();
    },
    [applyPendingOutfit],
  );

  const disconnect = useCallback(() => {
    pendingOutfitRef.current = null;
    const client = clientRef.current;
    clientRef.current = null;
    client?.disconnect();
    setRemoteStream(null);
    setStatus("CLOSED");
  }, []);

  useEffect(() => {
    if (!enabled || !cameraStream) {
      if (clientRef.current) disconnect();
      return;
    }

    const abortController = new AbortController();
    let client: RealTimeClient | null = null;
    let watchdog: ReturnType<typeof setTimeout> | null = null;
    const sourceStream = cameraStream;

    setStatus("CONNECTING");

    async function connect() {
      try {
        const { clientToken } = await issueDemoDecartToken();
        if (abortController.signal.aborted) return;

        const decart = createDecartClient({ apiKey: clientToken });
        client = await decart.realtime.connect(
          new MediaStream(sourceStream.getVideoTracks()),
          {
            model: DEMO_DECART_MODEL,
            resolution: "1080p",
            preferredVideoCodec: "vp8",
            onRemoteStream: (stream) => {
              if (!abortController.signal.aborted) setRemoteStream(stream);
            },
          },
        );

        // Cleanup can abort while the asynchronous connection is still pending.
        // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
        if (abortController.signal.aborted) {
          client.disconnect();
          return;
        }

        client.on("connectionChange", (state) => {
          if (!abortController.signal.aborted) {
            setStatus(mapConnectionState(state));
          }
        });
        client.on("error", (error) => {
          if (abortController.signal.aborted) return;
          console.error("Demo Decart connection error:", error);
          setStatus("ERROR");
        });

        clientRef.current = client;
        setStatus(mapConnectionState(client.getConnectionState()));
        void applyPendingOutfit();

        watchdog = setTimeout(disconnect, DEMO_TIMING.decartConnectionMs);
      } catch (error) {
        if (abortController.signal.aborted) return;
        console.error("Failed to connect demo Decart:", error);
        setStatus("ERROR");
      }
    }

    void connect();

    return () => {
      abortController.abort();
      if (watchdog) clearTimeout(watchdog);
      clientRef.current = null;
      client?.disconnect();
      setRemoteStream(null);
    };
  }, [enabled, cameraStream, applyPendingOutfit, disconnect]);

  return { status, remoteStream, applyOutfit, disconnect };
}
