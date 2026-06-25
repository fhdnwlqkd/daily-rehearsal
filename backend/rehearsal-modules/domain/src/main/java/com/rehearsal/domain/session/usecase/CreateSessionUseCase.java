package com.rehearsal.domain.session.usecase;

import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.command.CreateSessionCommand;

public interface CreateSessionUseCase {

  ClientSession createSession(CreateSessionCommand command);
}
