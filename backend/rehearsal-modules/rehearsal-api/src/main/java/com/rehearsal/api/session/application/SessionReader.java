package com.rehearsal.api.session.application;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionReader {

  private final SessionCache sessionCache;

  public ClientSession get(String sessionId) {
    return sessionCache
        .findById(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
  }
}
