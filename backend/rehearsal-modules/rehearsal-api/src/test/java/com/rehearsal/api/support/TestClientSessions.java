package com.rehearsal.api.support;

import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.situation.model.SituationType;

public final class TestClientSessions {

  private TestClientSessions() {}

  public static ClientSession sessionWith(SessionStatus status) {
    return ClientSession.builder()
        .sessionId("test-session-id")
        .situationType(SituationType.DATE)
        .status(status)
        .contextStatus(ContextStatus.COMPLETED)
        .selectedOutfitId("test-outfit-id")
        .build();
  }
}
