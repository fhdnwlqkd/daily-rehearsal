package com.rehearsal.domain.rehearsal.usecase;

import com.rehearsal.domain.rehearsal.model.SimulationTurn;

public interface SubmitNextOpponentLineUseCase {

  SimulationTurn submitNextLine(String sessionId, int turnNo);
}
