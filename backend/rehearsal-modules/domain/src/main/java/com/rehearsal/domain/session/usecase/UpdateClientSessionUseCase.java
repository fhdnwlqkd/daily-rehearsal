package com.rehearsal.domain.session.usecase;

import com.rehearsal.domain.session.model.ClientSession;

public interface UpdateClientSessionUseCase {

  ClientSession submitBriefing(String sessionId, String transcript);
}
