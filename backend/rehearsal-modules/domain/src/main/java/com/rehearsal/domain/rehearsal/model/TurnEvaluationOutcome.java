package com.rehearsal.domain.rehearsal.model;

public enum TurnEvaluationOutcome {
  ACCEPTED,
  RETRY_REQUIRED,
  FORCED_ADVANCE;

  public boolean advancesTurn() {
    return this == ACCEPTED || this == FORCED_ADVANCE;
  }
}
