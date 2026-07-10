package com.rehearsal.datasource.cache.session;

import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
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
    String selectedOutfitId,
    int currentTurn,
    int maxTurn,
    String currentOpponentLine,
    List<ConversationHistory> conversationHistory,
    List<TurnEvaluation> turnEvaluations) {

  public static ClientSessionRedisEntity from(ClientSession session) {
    return new ClientSessionRedisEntity(
        session.getSessionId(),
        session.getSituationType().key(),
        session.getStatus(),
        session.getContextStatus(),
        session.getFollowUpAttempt(),
        contextValues(session.getPartialContext()),
        contextValues(session.getFinalContext()),
        session.getMissingSlotKeys(),
        session.getFollowUpQuestions(),
        session.getSelectedOutfitId(),
        session.getCurrentTurn(),
        session.getMaxTurn(),
        session.getCurrentOpponentLine(),
        session.getConversationHistory(),
        session.getTurnEvaluations());
  }

  public ClientSession toDomain() {
    SituationType restoredSituationType = restoreSituationType();
    return ClientSession.builder()
        .sessionId(sessionId)
        .situationType(restoredSituationType)
        .status(status)
        .contextStatus(contextStatus)
        .followUpAttempt(followUpAttempt)
        .partialContext(restoreContext(restoredSituationType, partialContext))
        .finalContext(restoreContext(restoredSituationType, finalContext))
        .missingSlotKeys(missingSlotKeys)
        .followUpQuestions(followUpQuestions)
        .selectedOutfitId(selectedOutfitId)
        .currentTurn(currentTurn)
        .maxTurn(maxTurn)
        .currentOpponentLine(currentOpponentLine)
        .conversationHistory(conversationHistory)
        .turnEvaluations(turnEvaluations)
        .build();
  }

  private SituationType restoreSituationType() {
    if (situationType == null || situationType.isBlank()) {
      return SituationType.DATE;
    }
    return SituationType.fromKey(situationType);
  }

  private static Map<String, Object> contextValues(SessionContext context) {
    return context == null ? null : context.valuesWithSituationType();
  }

  private static SessionContext restoreContext(
      SituationType situationType, Map<String, Object> context) {
    return context == null ? null : SessionContext.from(situationType, context);
  }
}
