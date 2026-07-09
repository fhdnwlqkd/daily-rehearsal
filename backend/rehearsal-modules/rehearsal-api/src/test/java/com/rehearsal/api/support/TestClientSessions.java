package com.rehearsal.api.support;

import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.Map;

public final class TestClientSessions {

  private TestClientSessions() {}

  public static ClientSession sessionWith(SessionStatus status) {
    return ClientSession.builder()
        .sessionId("test-session-id")
        .situationType(SituationType.DATE)
        .status(status)
        .contextStatus(ContextStatus.COMPLETED)
        .partialContext(SessionContext.empty(SituationType.DATE))
        .finalContext(SessionContext.from(SituationType.DATE, Map.of("situationType", "date")))
        .selectedOutfitId("test-outfit-id")
        .build();
  }
}
