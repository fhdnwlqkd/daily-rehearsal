package com.rehearsal.domain.rehearsal.usecase;

import com.rehearsal.domain.rehearsal.model.OpponentLineJob;

public interface SubmitNextOpponentLineUseCase {

  OpponentLineJob submitNextLine(String sessionId, int turnNo);
}
