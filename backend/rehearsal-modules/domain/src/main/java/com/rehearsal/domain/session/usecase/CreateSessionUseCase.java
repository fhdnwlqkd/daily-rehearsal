package com.rehearsal.domain.session.usecase;

import com.rehearsal.domain.session.model.ClientSession;

public interface CreateSessionUseCase {

  ClientSession createSession(String channel);
}
