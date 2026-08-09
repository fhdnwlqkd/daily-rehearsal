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
            "인사와 자신의 역할 또는 함께 일하게 된 소감을 전한다."),
        List.of(
            "밝게 인사하고 자신의 역할이나 기대를 소개한다.",
            "업무나 조직에 관한 질문에 솔직하고 협업적인 태도로 답한다.",
            "예상하지 못한 질문에도 유쾌하고 예의 있게 대응한다."),
        "먼저 다가가고 함께 일하려는 밝고 협업적인 태도",
        "역할이나 관심사를 묻는 쉬운 질문으로 긴장을 낮춘다.",
        new SimulationTurnPlan(
            "팀원이 가볍게 말을 걸며 적응을 돕습니다.",
            "오늘 둘러보면서 궁금했던 점은 없었어요?",
            "궁금한 점 하나를 솔직하게 말하고 도움에 감사를 표현해보세요.",
            "궁금한 점을 말하고 도움에 대한 감사를 표현한다."));
  }
}
