package com.rehearsal.api.session.application;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionReader {

  private final SessionRepository sessionRepository;

  public ClientSession get(String sessionId) {
    return sessionRepository
        .findSession(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
  }

  public ClientSession getForUpdate(String sessionId) {
    return sessionRepository
        .findSessionForUpdate(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
  }
}
