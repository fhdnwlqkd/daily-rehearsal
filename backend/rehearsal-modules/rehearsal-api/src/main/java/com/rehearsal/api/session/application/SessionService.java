package com.rehearsal.api.session.application;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Description("Application service for P1 client session create, get, and briefing submit flow")
@Service
@RequiredArgsConstructor
public class SessionService
    implements CreateSessionUseCase, GetSessionUseCase, UpdateClientSessionUseCase {

  private final SessionCache sessionCache;

  @Override
  public ClientSession createSession(SituationType situationType) {
    ClientSession session = ClientSession.create(situationType);
    return sessionCache.save(session);
  }

  @Override
  public ClientSession getSession(String sessionId) {
    return sessionCache
        .findById(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
  }

  @Override
  public ClientSession submitBriefing(String sessionId, String transcript) {
    ClientSession session = getSession(sessionId);

    session.startContextExtraction();
    return sessionCache.save(session);
  }
}
