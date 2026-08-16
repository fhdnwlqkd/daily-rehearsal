package com.rehearsal.domain.rehearsal.registry.type;

import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;

public final class FirstDayRehearsalConfig {

  private FirstDayRehearsalConfig() {}

  public static RehearsalConfigDefinition definition() {
    return new RehearsalConfigDefinition(
        SituationType.FIRST_DAY,
        3,
        new SimulationTurnPlan(
            "첫 출근한 사무실에서 팀원들이 당신을 바라보고 있습니다.",
            "오늘부터 함께 일하게 됐다고 들었어요. 간단히 소개해주시겠어요?",
            "밝게 인사하고 맡게 된 역할이나 기대를 짧게 소개해보세요.",
            "인사를 하거나 자신의 역할, 함께 일하게 된 소감 중 하나를 자연스럽게 표현한다."),
        List.of(
            """
            첫 출근의 고정 자기소개 턴이다. 밝게 인사하거나 자신의 역할·기대·소감 중 하나를
            자연스럽게 표현해 팀원들이 편안함을 느끼게 한다.
            """
                .strip(),
            """
            첫 출근 2턴인 업무·조직 적응 질문 대응을 설계한다.
            - 소재는 ACCEPTED_CONVERSATION_HISTORY에서 사용자가 실제로 말한 내용을 우선하고,
              없으면 업무 방식·협업 방식·팀 분위기 같은 일반적인 적응 질문 중 하나를 사용한다.
            - critical_moment는 사용자가 가장 걱정하거나 연습하고 싶은 순간이다. 현재 흐름에
              자연스럽고 사용자에게 도움이 될 때만 질문 소재나 난도 조절에 사용한다.
            - desired_persona는 상대 말투와 행동 안내의 톤을 조절하는 힌트다. 사용자가 그
              인상을 이미 주었다고 단정하지 않는다.
            - sceneCue는 1턴 직후 상황을 새 사실 없이 한 문장으로 쓴다.
            - opponentLine은 한 가지 안전한 소재만 사용한 자연스러운 한국어 1~2문장으로
              만들고 질문은 하나만 한다. 이미 답한 내용을 반복해서 묻지 않는다.
            - actionPrompt는 다음 문장을 그대로 사용한다: "업무나 조직에 관한 질문에
              솔직하고 협업적인 태도로 답해보세요."
            - acceptedIntentHint는 다음 문장을 그대로 사용한다: "질문과 관련된 솔직한 대답,
              협업적인 태도, 배우려는 자세 중 하나 이상이 있으면 통과한다. 단순한 네·아니요,
              진행 방법을 묻는 말, 질문과 무관한 답변은 통과하지 않는다."
            - 짧거나 서툴러도 질문에 맞는 정보가 있으면 통과할 수 있게 설계한다.
            - 나이, 성별, 학력, 이전 직장, 연봉을 추정하거나 캐묻지 않는다.
            - selectedOutfit과 outfit_direction은 장면, 질문, 행동 요구, 통과 기준에
              사용하지 않는다.
            """
                .strip(),
            """
            첫 출근 3턴인 돌발 질문 대응을 설계한다.
            - critical_moment가 있으면 그 걱정되는 순간과 자연스럽게 연결되는 예상 밖 질문이나
              상황을 하나 사용한다. 없으면 무례하지 않은 범위의 가벼운 돌발 질문(사소한 부탁,
              취향, 가벼운 실수담 등)을 사용한다.
            - ACCEPTED_CONVERSATION_HISTORY의 사실은 하나만 자연스럽게 반영하고, 없으면
              만들지 않는다.
            - sceneCue는 대화가 자연스럽게 이어지는 흐름임을 새 사실 없이 한 문장으로 쓴다.
            - opponentLine은 상대가 가볍게 건네는 자연스러운 한국어 1~2문장이며 질문은 최대
              하나다.
            - actionPrompt는 다음 문장을 그대로 사용한다: "예상하지 못한 질문에도 방어적이지
              않게 유쾌하고 예의 있게 대응해보세요."
            - acceptedIntentHint는 다음 문장을 그대로 사용한다: "당황하지 않고 예의 있게
              반응하거나, 유쾌하게 받아넘기거나, 솔직하게 답하는 것 중 하나 이상이 있으면
              통과한다. 무시하거나 질문과 무관한 답변은 통과하지 않는다."
            - 짧거나 재치가 완벽하지 않아도 무례하지 않고 상황에 맞으면 통과할 수 있게
              설계한다.
            - 나이, 성별, 학력, 이전 직장, 연봉을 추정하거나 캐묻지 않는다.
            - selectedOutfit과 outfit_direction은 장면, 질문, 행동 요구, 통과 기준에
              사용하지 않는다.
            """
                .strip()),
        """
        표현의 완성도보다 먼저 다가가고 함께 일하려는 밝고 협업적인 태도를 우선한다.
        ACTION_PROMPT와 ACCEPTED_INTENT_HINT가 현재 턴의 최소 통과 기준이며, 요구된 선택
        행동 중 하나를 수행하면 다른 행동이 없다는 이유로 실패시키지 않는다.
        맞춤법, 존댓말, 길이가 완벽하지 않아도 최소 의도가 있으면 통과시킨다. metrics는
        참고만 하고 transcript의 통과 결과를 뒤집지 않는다.
        desired_persona는 코칭의 방향이지 통과를 위한 연기 수준이 아니다. selectedOutfit과
        outfit_direction은 답변 평가나 피드백 근거로 사용하지 않는다.
        피드백은 잘한 행동 또는 부족한 이유를 먼저 짚고, 사용자가 다음 시도에서 바로 고칠
        행동 하나만 밝고 실용적인 한국어로 제안한다.
        """
            .strip(),
        """
        실패한 사용자 답변이 실제로 있었던 일처럼 취급하거나 팀원이 실망한 것처럼 반응하지
        않는다.
        현재 턴의 원래 목표와 최소 통과 의도를 유지하고, 앞선 ACCEPTED 대화에서 확인된
        사실만 이어받는다.
        질문 범위를 역할이나 관심사를 묻는 쉬운 질문 하나로 좁혀 대화 진입 난도를 낮춘다.
        나이, 성별, 학력, 이전 직장, 연봉을 추정하지 않으며 selectedOutfit과
        outfit_direction을 사용하지 않는다.
        """
            .strip(),
        new SimulationTurnPlan(
            "팀원이 가볍게 말을 걸며 적응을 돕습니다.",
            "오늘 둘러보면서 궁금했던 점은 없었어요?",
            "궁금한 점 하나를 솔직하게 말하고 도움에 감사를 표현해보세요.",
            "궁금한 점을 말하고 도움에 대한 감사를 표현한다."));
  }
}
