package com.rehearsal.domain.rehearsal.usecase;

import com.rehearsal.domain.rehearsal.model.SimulationTurn;

public interface GetNextOpponentLineUseCase {

  SimulationTurn getNextLine(String sessionId, int turnNo);
}
