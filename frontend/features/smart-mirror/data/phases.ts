import type { ExperiencePhase } from "../types";

export const experiencePhases = [
  { id: "briefing", label: "거울 속의 나", timeRange: "0-15s" },
  { id: "context", label: "데이터 스캐닝", timeRange: "15-25s" },
  { id: "transformation", label: "시공간의 전환", timeRange: "25-35s" },
  { id: "gesture-fit", label: "제스처 피팅", timeRange: "35-45s" },
  { id: "rehearsal", label: "결정적 순간", timeRange: "45-55s" },
  { id: "change-card", label: "변화 카드", timeRange: "55-60s" },
] as const satisfies readonly ExperiencePhase[];
