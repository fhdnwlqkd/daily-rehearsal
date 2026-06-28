package com.rehearsal.domain.session.usecase;

import com.rehearsal.domain.session.model.ClientSession;

public interface GetSessionUseCase {

  ClientSession getSession(String sessionId);
}
