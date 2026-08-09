import type { ExperiencePhase } from "../types";

export const experiencePhases = [
  { id: "type-select", label: "타입 선택" },
  { id: "briefing", label: "브리핑" },
  { id: "outfit", label: "옷 입히기" },
  { id: "simulation", label: "시뮬레이션" },
  { id: "ticket", label: "티켓 발급" },
] as const satisfies readonly ExperiencePhase[];
