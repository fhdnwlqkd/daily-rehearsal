"use client";

import { useCallback, useEffect, useState } from "react";
import {
  advanceDemoFlow,
  confirmDemoOutfit,
  initialDemoFlowSnapshot,
  timedDemoTransition,
} from "../lib/demo-flow";
import type { DemoOutfit } from "../types";

export function useDemoFlow() {
  const [snapshot, setSnapshot] = useState(initialDemoFlowSnapshot);

  useEffect(() => {
    const transition = timedDemoTransition(snapshot);
    if (!transition) return;
    const timer = setTimeout(
      () => setSnapshot(transition.next),
      transition.delayMs,
    );
    return () => clearTimeout(timer);
  }, [snapshot]);

  const advance = useCallback(() => {
    setSnapshot(advanceDemoFlow);
  }, []);

  const confirmOutfit = useCallback((outfit: DemoOutfit) => {
    setSnapshot((current) => confirmDemoOutfit(current, outfit));
  }, []);

  return { ...snapshot, advance, confirmOutfit };
}
