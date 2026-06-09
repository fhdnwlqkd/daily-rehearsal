package com.rehearsal.api.session.service;

import com.rehearsal.api.common.exception.BusinessException;
import com.rehearsal.api.common.exception.ErrorCode;
import com.rehearsal.api.session.contract.SessionContract.CreateSessionCommand;
import com.rehearsal.api.session.contract.SessionContract.CreateSessionResult;
import com.rehearsal.api.session.contract.SessionContract.GetSessionResult;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

  private final InMemorySessionStore sessionStore;

  public SessionService(InMemorySessionStore sessionStore) {
    this.sessionStore = sessionStore;
  }

  public CreateSessionResult create(CreateSessionCommand command) {
    SessionState sessionState = SessionState.create(command.channel());
    sessionStore.save(sessionState);
    return CreateSessionResult.from(sessionState);
  }

  public GetSessionResult get(String sessionId) {
    SessionState sessionState =
        sessionStore
            .findById(sessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    return GetSessionResult.from(sessionState);
  }
}
