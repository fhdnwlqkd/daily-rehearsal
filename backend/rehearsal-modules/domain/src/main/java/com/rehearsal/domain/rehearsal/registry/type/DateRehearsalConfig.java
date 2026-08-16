package com.rehearsal.domain.rehearsal.registry.type;

import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;

public final class DateRehearsalConfig {

  private DateRehearsalConfig() {}

  public static RehearsalConfigDefinition definition() {
    return new RehearsalConfigDefinition(
        SituationType.DATE,
        3,
        new SimulationTurnPlan(
            "소개팅 상대가 약속 장소에 도착해 자리에 앉았습니다.",
            "안녕하세요. 기다리게 한 건 아니죠?",
            "상대가 편안함을 느끼도록 자연스럽게 첫인사를 건네보세요.",
            "상황에 맞는 인사를 하거나 상대를 안심시키는 배려를 표현한다."),
        List.of(
            """
            소개팅의 고정 첫인사 턴이다. 상황에 맞는 인사나 배려를 표현해 상대가 편안함을 느끼게 한다.
            """
                .strip(),
            """
            소개팅 2턴인 상호적인 대화를 설계한다.
            - 소재 우선순위는 FINAL_CONTEXT의 conversation_material, ACCEPTED_CONVERSATION_HISTORY에서 사용자가 실제로 말한 소재, situation_detail 순서다.
            - 위 우선순위에 쓸 구체 정보가 없으면 '오늘 오는 길', '요즘 편하게 즐기는 일'처럼 누구나 현재 기준으로 답할 수 있는 가벼운 소재 하나를 사용한다.
            - counterpart_context, interaction_setting, prior_interaction_context는 명시된 사실일 때만 자연스러운 배경으로 쓴다.
            - critical_moment는 첫 대화를 결정하는 필수값이 아니다. 현재 흐름에 자연스럽고 사용자에게 도움이 될 때만 난도 조절에 사용한다.
            - desired_persona와 response_style은 상대 말투와 행동 안내의 톤을 조절하는 힌트다. 사용자가 그 인상을 이미 주었다고 단정하지 않는다.
            - sceneCue는 1턴 직후 상황을 새 사실 없이 한 문장으로 쓴다.
            - opponentLine은 한 가지 안전한 소재만 사용한 자연스러운 한국어 1~2문장으로 만들고 질문은 하나만 한다. 이미 답한 내용을 반복해서 묻지 않는다.
            - 선택지를 여러 개 나열하거나 취미·경험·이유를 한꺼번에 요구하지 않는다.
            - actionPrompt는 다음 문장을 그대로 사용한다: "상대의 질문에 구체적으로 답하거나, 관련된 경험을 말하거나, 같은 주제로 되물어 대화를 이어가 보세요."
            - acceptedIntentHint는 다음 문장을 그대로 사용한다: "상대 질문과 관련된 구체적인 내용, 관련 경험이나 취향, 같은 주제의 되묻기 중 하나 이상이 있으면 통과한다. 단순한 네·아니요, 진행 방법을 묻는 말, 질문과 무관한 답변은 통과하지 않는다."
            - 짧거나 서툴러도 질문에 맞는 정보가 있으면 통과할 수 있게 설계한다.
            - 성별, 나이, 외모, 직업, 재산, 연애 경험을 추정하거나 캐묻지 않는다.
            - selectedOutfit과 outfit_direction은 장면, 질문, 행동 요구, 통과 기준에 사용하지 않는다.
            """
                .strip(),
            """
            소개팅 3턴인 부담 없는 마무리를 설계한다.
            - desired_outcome과 ACCEPTED_CONVERSATION_HISTORY를 우선 사용한다. 앞선 대화의 사실은 하나만 자연스럽게 반영하고, 없으면 만들지 않는다.
            - desired_outcome이 다음 만남이나 연락을 포함하더라도 상대가 먼저 호감이나 약속을 확정한 것처럼 만들지 않는다.
            - sceneCue는 만남을 마무리할 시점임을 새 사실 없이 한 문장으로 쓴다.
            - opponentLine은 상대가 먼저 건네는 자연스러운 한국어 1~2문장의 마무리 말이다. 질문은 필수가 아니다.
            - actionPrompt는 다음 문장을 그대로 사용한다: "감사, 즐거웠다는 느낌, 관심, 다음 만남 의향 또는 자연스러운 작별 중 지금 원하는 한 가지를 표현해 보세요."
            - acceptedIntentHint는 다음 문장을 그대로 사용한다: "감사, 긍정적인 느낌, 관심, 다음 만남 의향, 예의 있는 작별 중 하나 이상을 현재 상황에 맞게 표현하면 통과한다. 다음 만남을 제안하지 않았다는 이유만으로 실패시키지 않는다."
            - 감사나 작별만 말한 짧은 답변도 통과할 수 있게 설계한다.
            - interaction_constraint가 있으면 피해야 할 주제나 표현을 상대 발화에도 적용한다.
            - selectedOutfit과 outfit_direction은 장면, 상대의 호감, 행동 요구, 통과 기준에 사용하지 않는다.
            """
                .strip()),
        """
        표현의 화려함보다 현재 장면에 맞는 배려와 대화를 이어가려는 의도를 우선한다.
        ACTION_PROMPT와 ACCEPTED_INTENT_HINT가 현재 턴의 최소 통과 기준이며, 요구된 선택 행동 중 하나를 수행하면 다른 행동이 없다는 이유로 실패시키지 않는다.
        맞춤법, 존댓말, 길이가 완벽하지 않아도 최소 의도가 있으면 통과시킨다. metrics는 참고만 하고 transcript의 통과 결과를 뒤집지 않는다.
        desired_persona와 response_style은 코칭의 방향이지 통과를 위한 연기 수준이 아니다. selectedOutfit과 outfit_direction은 답변 평가나 피드백 근거로 사용하지 않는다.
        interaction_constraint를 어긴 경우에는 그것이 명시적이고 현재 답변과 직접 관련될 때만 반영한다.
        피드백은 잘한 행동 또는 부족한 이유를 먼저 짚고, 사용자가 다음 시도에서 바로 고칠 행동 하나만 따뜻한 한국어로 제안한다.
        """
            .strip(),
        """
        실패한 사용자 답변이 실제 대화에서 일어난 것처럼 취급하거나 상대가 거절당한 것처럼 반응하지 않는다.
        현재 턴의 원래 목표와 최소 통과 의도를 유지하고, 앞선 ACCEPTED 대화에서 확인된 사실만 이어받는다.
        질문 범위를 한 가지 안전한 소재로 좁히고 상대 발화와 행동 안내를 더 짧고 분명하게 만들어 대화 진입 난도를 낮춘다.
        성별, 외모, 직업, 재산, 연애 경험을 추정하지 않으며 selectedOutfit과 outfit_direction을 사용하지 않는다.
        """
            .strip(),
        new SimulationTurnPlan(
            "대화를 나누던 중 상대가 편안하게 말을 이어갑니다.",
            "오늘 이렇게 이야기 나누는 건 어떠세요?",
            "지금 느끼는 점을 짧게 말하거나 상대에게도 자연스럽게 물어보세요.",
            "현재 느낌을 표현하거나 같은 주제로 상대에게 되묻는다."));
  }
}
