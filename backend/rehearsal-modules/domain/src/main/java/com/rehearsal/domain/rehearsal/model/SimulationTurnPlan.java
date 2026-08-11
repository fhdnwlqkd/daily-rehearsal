package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("한 시뮬레이션 턴에서 제시할 장면, 상대 발화, 행동 요구와 통과 의도")
public record SimulationTurnPlan(
    String sceneCue, String opponentLine, String actionPrompt, String acceptedIntentHint) {

  public SimulationTurnPlan {
    sceneCue = requireText(sceneCue, "sceneCue");
    opponentLine = requireText(opponentLine, "opponentLine");
    actionPrompt = requireText(actionPrompt, "actionPrompt");
    acceptedIntentHint = requireText(acceptedIntentHint, "acceptedIntentHint");
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
