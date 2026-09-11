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
    question: "내일 어떤 상황을 미리 연습해보고 싶으세요?",
    example: "중요한 발표나 면접처럼, 미리 연습하고 싶은 장면을 말해주세요.",
    transcript:
      "내일 삼성 라이프놀로지랩 최종 발표가 있는데, 제가 팀의 발표를 맡았어요.",
  },
  followUp: {
    question: "발표에서 가장 걱정되는 순간은 언제인가요?",
    transcript:
      "시작할 때 너무 긴장해서 첫 문장을 잊어버리거나, 저도 모르게 말을 빨리할까 봐 걱정돼요.",
  },
} as const;

export const demoSimulationTurns = [
  {
    sceneCue: "삼성 라이프놀로지랩 최종 발표를 위해 무대 앞에 섰습니다.",
    opponentLine: "준비되셨으면 발표를 시작해 주세요.",
    actionPrompt: "청중을 바라보고 첫 문장으로 서비스의 핵심을 소개해보세요.",
    transcript:
      "안녕하세요. 어... 저희가 준비한 건 데일리 리허설이라는 서비스고요. 일단 준비한 내용부터 말씀드리겠습니다.",
    outcome: "COACHING",
    feedback:
      "머뭇거림 때문에 발표의 핵심이 바로 드러나지 않았어요. 서비스가 해결하는 문제부터 한 문장으로 시작해보세요.",
  },
  {
    sceneCue: "한 번 호흡을 가다듬고 첫 문장을 다시 시작합니다.",
    opponentLine:
      "좋습니다. 이 서비스가 누구에게 왜 필요한지 한 문장으로 말해볼까요?",
    actionPrompt: "대상과 해결하려는 문제를 먼저 전달해보세요.",
    transcript:
      "중요한 순간을 앞두고도 충분히 연습하지 못하는 사람들을 위해, AI가 내일의 상황을 미리 경험하게 해주는 서비스, 데일리 리허설입니다.",
    outcome: "ACCEPTED",
    feedback:
      "좋아요. 누구를 위한 서비스인지와 해결하는 문제가 첫 문장에 담겨 발표의 방향이 분명하게 전달됐어요.",
  },
] as const satisfies readonly DemoSimulationTurn[];
