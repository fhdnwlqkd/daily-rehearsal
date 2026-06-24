import type { ExperienceData } from "../types";

export const mockExperience: ExperienceData = {
  transcript:
    "내일 소개팅이 있는데 좀 어색할 것 같고, 너무 꾸민 느낌은 싫어요. 성수 쪽 음식점에서 만나고 늦으면 안 될 것 같아요.",
  contextReply:
    "자연스럽고 자신감 있어 보이고 싶어요. 처음 만났을 때 어색한 침묵이 생기는 게 제일 걱정돼요. 상대는 차분하지만 낯가림이 있을 것 같아요.",
  tags: ["소개팅", "어색함", "과하지 않은 단정함", "성수 음식점", "지각 우려"],
  missing: ["페르소나", "걱정 순간", "상대 분위기"],
  followUpQuestions: [
    "어떤 모습으로 보이고 싶나요?",
    "가장 걱정되는 순간은 언제인가요?",
    "상대는 어떤 분위기일 것 같나요?",
  ],
  outfits: [
    { name: "단정한 캐주얼", tone: "과하지 않은 첫인상", active: false },
    { name: "부드러운 뉴트럴", tone: "편안한 대화 분위기", active: true },
    { name: "차분한 데이트룩", tone: "조용하지만 선명한 인상", active: false },
  ],
  persona: "자신감 있는 첫인상",
  routeRisk: "비 옴 +12분",
  placeMood: "공간 소음도 중간",
  gestureHint: "오른손을 넘기면 다음 스타일, 손바닥을 멈추면 선택",
  aiPrompt: "처음 뵙네요. 오는 길 괜찮으셨어요?",
  userReply: "네, 조금 일찍 나와서 여유 있게 도착했어요. 여기 분위기가 좋네요.",
  changeAction: "20분 일찍 출발하기",
  changeAttitude: "천천히 말하고 먼저 웃기",
  ifThen: "어색한 침묵이 생기면 장소에 대한 질문으로 다시 시작하기",
};
