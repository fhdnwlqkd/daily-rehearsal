package com.rehearsal.domain.session.usecase;

import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.situation.model.SituationType;

public interface CreateSessionUseCase {

  ClientSession createSession(SituationType situationType);
}
