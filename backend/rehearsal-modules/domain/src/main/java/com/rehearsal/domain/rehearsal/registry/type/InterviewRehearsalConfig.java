package com.rehearsal.domain.rehearsal.registry.type;

import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;

public final class InterviewRehearsalConfig {

  private InterviewRehearsalConfig() {}

  public static RehearsalConfigDefinition definition() {
    return new RehearsalConfigDefinition(
        SituationType.INTERVIEW,
        3,
        2,
        new SimulationTurnPlan(
            "면접실에 들어가 면접관과 마주 앉았습니다.",
            "반갑습니다. 먼저 간단하게 자기소개 부탁드립니다.",
            "지원 직무와 강점이 드러나도록 짧고 또렷하게 자기소개해보세요.",
            "자신의 역할 또는 강점과 지원 맥락을 설명한다."),
        List.of(
            "지원 직무와 강점이 드러나는 자기소개를 한다.",
            "경험을 묻는 구체적인 질문에 근거를 들어 답한다.",
            "까다로운 후속 질문에도 핵심을 잃지 않고 침착하게 답한다."),
        "질문의 핵심에 답하는 구조, 구체적인 근거와 침착함",
        "범위를 좁힌 질문으로 한 가지 경험부터 설명하게 한다.",
        new SimulationTurnPlan(
            "면접관이 답변 범위를 좁혀 다시 질문합니다.",
            "가장 자신 있게 설명할 수 있는 경험 하나를 말씀해주시겠어요?",
            "상황과 본인의 역할, 결과를 중심으로 답해보세요.",
            "한 가지 경험에서 자신의 역할과 결과를 설명한다."));
  }
}
