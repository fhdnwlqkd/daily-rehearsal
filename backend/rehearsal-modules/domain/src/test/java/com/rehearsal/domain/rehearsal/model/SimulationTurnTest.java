package com.rehearsal.domain.rehearsal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

class SimulationTurnTest {

  @Test
  void completesPendingOpponentLine() {
    SimulationTurn turn = SimulationTurn.pending("session-id", 2, TurnGenerationMode.NORMAL);
    SimulationTurnPlan plan = plan("next opponent line");

    turn.complete(plan);

    assertThat(turn.getOpponentLineStatus()).isEqualTo(OpponentLineStatus.COMPLETED);
    assertThat(turn.getPlan()).isEqualTo(plan);
    assertThat(turn.getFailureReason()).isNull();
  }

  @Test
  void rejectsCompletingAnAlreadyCompletedTurn() {
    SimulationTurn turn =
        SimulationTurn.completed("session-id", 1, TurnGenerationMode.STATIC, plan("first line"));

    assertThatThrownBy(() -> turn.complete(plan("another line")))
        .isInstanceOf(BusinessException.class);
  }

  private SimulationTurnPlan plan(String opponentLine) {
    return new SimulationTurnPlan("scene", opponentLine, "action prompt", "accepted intent hint");
  }
}
