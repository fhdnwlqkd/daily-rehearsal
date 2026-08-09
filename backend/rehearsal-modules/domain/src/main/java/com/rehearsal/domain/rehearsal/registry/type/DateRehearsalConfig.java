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
            "인사와 자기소개 또는 상대를 배려하는 말을 한다."),
        List.of(
            "상대가 편안함을 느끼는 첫인사를 건넨다.",
            "상대의 질문을 듣고 부담스럽지 않게 대화를 이어간다.",
            "관심을 표현하며 다음 만남으로 이어질 수 있게 자연스럽게 마무리한다."),
        "상대를 배려하며 대화를 자연스럽게 이어가는 태도",
        "답하기 쉬운 일상 질문으로 대화 진입 난도를 낮춘다.",
        new SimulationTurnPlan(
            "대화가 잠시 끊겨 상대가 편하게 말을 이어갑니다.",
            "오늘 여기까지 오는 길은 괜찮으셨어요?",
            "짧게 답한 뒤 상대에게도 자연스럽게 질문해보세요.",
            "질문에 답하고 상대에게 관련된 질문을 되돌린다."));
  }
}
