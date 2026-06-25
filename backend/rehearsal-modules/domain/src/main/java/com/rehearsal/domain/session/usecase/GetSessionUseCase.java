package com.rehearsal.domain.session.usecase;

import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.command.GetSessionCommand;

public interface GetSessionUseCase {

  ClientSession getSession(GetSessionCommand command);
}
