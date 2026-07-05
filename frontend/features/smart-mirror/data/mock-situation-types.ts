import type { GetSituationTypesResponse } from "../types";

/**
 * GetSituationTypesResponse 명세를 그대로 만족하는 mock.
 * 백엔드 레지스트리에 실제 등록된 데이터를 그대로 옮긴 것이다
 * (gestureOrder 오름차순). 타입 검사를 받으므로 명세가 바뀌면
 * 여기서 컴파일 에러로 드러난다.
 */
export const mockGetSituationTypesResponse: GetSituationTypesResponse = [
  {
    key: "date",
    label: "소개팅",
    gestureOrder: 1,
    briefingTitle: "내일의 소개팅을 짧게 말해주세요",
    exampleAnswers: ["내일 소개팅이 있는데 첫 인사가 어색할까 봐 걱정돼요."],
  },
  {
    key: "business_meeting",
    label: "비즈니스 미팅",
    gestureOrder: 2,
    briefingTitle: "내일의 비즈니스 미팅을 짧게 말해주세요",
    exampleAnswers: [
      "내일 고객 미팅에서 핵심 내용을 차분하게 전달하고 싶어요.",
    ],
  },
];
