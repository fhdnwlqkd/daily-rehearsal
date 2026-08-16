package com.rehearsal.domain.rehearsal.registry;

import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;

public record RehearsalConfigDefinition(
    SituationType situationType,
    int maxTurn,
    SimulationTurnPlan firstTurn,
    List<String> turnObjectives,
    String feedbackFocus,
    String recoveryDirection,
    SimulationTurnPlan technicalFallback) {

  public RehearsalConfigDefinition {
    turnObjectives = List.copyOf(turnObjectives);
    if (turnObjectives.size() != maxTurn) {
      throw new IllegalArgumentException("turnObjectives size must equal maxTurn");
    }
  }

  public String objectiveFor(int turnNo) {
    if (turnNo < 1 || turnNo > maxTurn) {
      throw new IllegalArgumentException("turnNo is outside the configured range: " + turnNo);
    }
    return turnObjectives.get(turnNo - 1);
  }

  public int maxAttemptsPerTurn() {
    return SimulationTurnAttempt.MAX_ATTEMPT;
  }
}
