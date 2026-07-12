package com.rehearsal.domain.rehearsal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

class SimulationTurnTest {

  @Test
  void completesPendingOpponentLine() {
    SimulationTurn turn = SimulationTurn.pending("session-id", 2);

    turn.complete("next opponent line");

    assertThat(turn.getOpponentLineStatus()).isEqualTo(OpponentLineStatus.COMPLETED);
    assertThat(turn.getOpponentLine()).isEqualTo("next opponent line");
    assertThat(turn.getFailureReason()).isNull();
  }

  @Test
  void rejectsCompletingAnAlreadyCompletedTurn() {
    SimulationTurn turn = SimulationTurn.completed("session-id", 1, "first line");

    assertThatThrownBy(() -> turn.complete("another line"))
        .isInstanceOf(BusinessException.class);
  }
}
