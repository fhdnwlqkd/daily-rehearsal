package com.rehearsal.datasource.cache.session;

import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;

public record ClientSessionRedisEntity(
    String sessionId,
    String situationType,
    SessionStatus status,
    ContextStatus contextStatus,
    int followUpAttempt,
    Map<String, Object> partialContext,
    Map<String, Object> finalContext,
    List<String> missingSlotKeys,
    List<String> followUpQuestions,
    String selectedOutfitId) {

  public static ClientSessionRedisEntity from(ClientSession session) {
    return new ClientSessionRedisEntity(
        session.getSessionId(),
        session.getSituationType().key(),
        session.getStatus(),
        session.getContextStatus(),
        session.getFollowUpAttempt(),
        session.getPartialContext(),
        session.getFinalContext(),
        session.getMissingSlotKeys(),
        session.getFollowUpQuestions(),
        session.getSelectedOutfitId());
  }

  public ClientSession toDomain() {
    return ClientSession.restore(
        sessionId,
        restoreSituationType(),
        status,
        contextStatus,
        followUpAttempt,
        partialContext,
        finalContext,
        missingSlotKeys,
        followUpQuestions,
        selectedOutfitId);
  }

  private SituationType restoreSituationType() {
    if (situationType == null || situationType.isBlank()) {
      return SituationType.DATE;
    }
    return SituationType.fromKey(situationType);
  }
}
