package com.rehearsal.domain.rehearsal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

class SimulationTurnAttemptTest {

  @Test
  void completesPendingEvaluation() {
    SimulationTurnAttempt attempt = SimulationTurnAttempt.pending(1L, 1, "user answer");

    attempt.complete(new TurnEvaluationResult(true, "good", false));

    assertThat(attempt.getEvaluationStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    assertThat(attempt.getSuccess()).isTrue();
    assertThat(attempt.getFeedback()).isEqualTo("good");
    assertThat(attempt.getFallback()).isFalse();
  }

  @Test
  void rejectsFailingAnAlreadyCompletedAttempt() {
    SimulationTurnAttempt attempt = SimulationTurnAttempt.pending(1L, 1, "user answer");
    attempt.complete(new TurnEvaluationResult(true, "good", false));

    assertThatThrownBy(() -> attempt.fail("late failure"))
        .isInstanceOf(BusinessException.class);
  }
}
