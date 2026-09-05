import type { DemoSimulationTurn } from "../types";

export const DEMO_TIMING = {
  transcriptRevealMs: 650,
  briefingAnalysisMs: 1800,
  followUpMergeMs: 1600,
  simulationIntroMs: 1800,
  evaluationMs: 1700,
  decartConnectionMs: 300_000,
} as const;

export const demoBriefing = {
  initial: {
    question: "내일 어떤 중요한 순간을 리허설하고 싶나요?",
    example: "중요한 발표나 면접처럼, 미리 연습하고 싶은 장면을 말해주세요.",
    transcript: "내일 굉장히 중요한 발표가 있어.",
  },
  followUp: {
    question: "발표에서 어떤 부분이 가장 걱정되는지 말해주세요.",
    transcript: "내일 발표를 시작할 때 긴장해서 말을 못할까 봐 걱정돼.",
  },
} as const;

export const demoSimulationTurns = [
  {
    sceneCue: "중요한 발표를 시작하기 위해 무대 중앙에 섰습니다.",
    opponentLine: "준비되셨으면 발표를 시작해주세요.",
    actionPrompt: "첫 문장으로 발표의 핵심을 전달해보세요.",
    transcript:
      "어... 오늘 제가 발표할 내용은 여러 가지가 있는데, 일단 설명부터 드리겠습니다.",
    outcome: "COACHING",
    feedback:
      "긴장한 표현이 반복되어 핵심이 늦게 전달됐어요. 첫 문장을 짧게 정리해볼게요.",
  },
  {
    sceneCue: "호흡을 가다듬고 발표의 첫 문장을 다시 시작합니다.",
    opponentLine:
      "좋습니다. 청중에게 가장 먼저 전하고 싶은 메시지는 무엇인가요?",
    actionPrompt: "결론을 먼저 말하고 이유를 이어서 설명해보세요.",
    transcript:
      "완벽한 준비보다 내일을 한 번 미리 살아보는 경험이 더 큰 변화를 만듭니다.",
    outcome: "ACCEPTED",
    feedback:
      "좋아요. 첫 문장에서 핵심 메시지를 분명하게 전달해 청중의 집중을 이끌었습니다.",
  },
] as const satisfies readonly DemoSimulationTurn[];
