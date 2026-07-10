package com.rehearsal.domain.rehearsal.usecase;

import com.rehearsal.domain.rehearsal.model.OpponentLineJob;

public interface GetNextOpponentLineUseCase {

  OpponentLineJob getNextLine(String sessionId, int turnNo);
}
