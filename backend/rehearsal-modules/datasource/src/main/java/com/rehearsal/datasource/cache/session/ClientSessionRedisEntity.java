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
    int attempt,
    String briefingTranscript,
    Map<String, Object> partialContext,
    Map<String, Object> finalContext,
    List<String> missingSlotKeys,
    List<String> followUpQuestions,
    String selectedOutfitId,
    int simulationTurn,
    List<Map<String, Object>> conversationHistory,
    List<Map<String, Object>> turnEvaluations,
    String videoUrl,
    Map<String, Object> ticket,
    String downloadUrl) {

  public static ClientSessionRedisEntity from(ClientSession session) {
    return new ClientSessionRedisEntity(
        session.getSessionId(),
        session.getSituationType().key(),
        session.getStatus(),
        session.getContextStatus(),
        session.getAttempt(),
        session.getBriefingTranscript(),
        session.getPartialContext(),
        session.getFinalContext(),
        session.getMissingSlotKeys(),
        session.getFollowUpQuestions(),
        session.getSelectedOutfitId(),
        session.getSimulationTurn(),
        session.getConversationHistory(),
        session.getTurnEvaluations(),
        session.getVideoUrl(),
        session.getTicket(),
        session.getDownloadUrl());
  }

  public ClientSession toDomain() {
    return ClientSession.restore(
        sessionId,
        restoreSituationType(),
        status,
        contextStatus,
        attempt,
        briefingTranscript,
        partialContext,
        finalContext,
        missingSlotKeys,
        followUpQuestions,
        selectedOutfitId,
        simulationTurn,
        conversationHistory,
        turnEvaluations,
        videoUrl,
        ticket,
        downloadUrl);
  }

  private SituationType restoreSituationType() {
    if (situationType == null || situationType.isBlank()) {
      return SituationType.DATE;
    }
    return SituationType.fromKey(situationType);
  }
}
