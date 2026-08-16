import type { ChangeCard, TicketSnapshot } from "../../types";

export type TicketPreviewSituation = "date" | "interview" | "first-day";

export const ticketPreviewData: Record<
  TicketPreviewSituation,
  { snapshot: TicketSnapshot; changeCard: ChangeCard }
> = {
  date: {
    snapshot: {
      situationLabel: "소개팅",
      criticalMoment: "첫 인사 뒤 대화가 잠시 끊기는 순간",
      desiredPersonaLabel: "차분하고 자연스러운 인상",
      selectedOutfitLabel: "단정한 네이비 재킷과 밝은 셔츠",
    },
    changeCard: {
      todayAction: "상대의 말을 끝까지 듣고, 답변 속 소재로 질문을 이어가기",
      tomorrowAttitude: "서두르지 않고 편안한 표정과 말하기 속도를 유지하기",
      ifThenPlan: "대화가 끊기면 호흡을 고른 뒤 오늘 가장 기대한 일을 묻기",
    },
  },
  interview: {
    snapshot: {
      situationLabel: "면접",
      criticalMoment: "프로젝트 기여도를 구체적으로 설명해야 하는 순간",
      desiredPersonaLabel: "차분하고 논리적인 인상",
      selectedOutfitLabel: "단정한 차콜 수트와 밝은 셔츠",
    },
    changeCard: {
      todayAction:
        "결론을 먼저 말한 뒤 내가 맡은 행동과 결과를 차례로 설명하기",
      tomorrowAttitude: "질문의 의도를 확인하고 짧게 생각한 뒤 또렷하게 답하기",
      ifThenPlan: "답변이 막히면 상황과 역할부터 나누어 한 문장씩 설명하기",
    },
  },
  "first-day": {
    snapshot: {
      situationLabel: "첫 출근",
      criticalMoment: "팀원들 앞에서 처음 자기소개를 시작하는 순간",
      desiredPersonaLabel: "밝고 신뢰할 수 있는 인상",
      selectedOutfitLabel: "단정한 셔츠와 차분한 슬랙스",
    },
    changeCard: {
      todayAction: "이름과 역할을 먼저 밝히고 함께하고 싶은 일을 짧게 전하기",
      tomorrowAttitude: "모르는 것은 솔직히 묻고 들은 내용은 한 번 확인하기",
      ifThenPlan: "갑작스러운 질문을 받으면 아는 범위를 말하고 확인을 약속하기",
    },
  },
};
