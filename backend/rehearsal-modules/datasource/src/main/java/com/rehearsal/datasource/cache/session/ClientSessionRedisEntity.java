package com.rehearsal.datasource.cache.session;

import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionStatus;
import java.util.List;
import java.util.Map;

public record ClientSessionRedisEntity(
    String sessionId,
    String channel,
    SessionStatus status,
    ContextStatus contextStatus,
    int followUpAttempt,
    String briefingTranscript,
    Map<String, Object> partialContext,
    Map<String, Object> finalUserContext,
    List<String> missingRequiredSlotKeys,
    String followUpQuestion,
    String selectedOutfitId,
    Map<String, Object> simulationDraft,
    Map<String, Object> feedbackResult,
    Map<String, Object> finalResult) {

  public static ClientSessionRedisEntity from(ClientSession session) {
    return new ClientSessionRedisEntity(
        session.getSessionId(),
        session.getChannel(),
        session.getStatus(),
        session.getContextStatus(),
        session.getFollowUpAttempt(),
        session.getBriefingTranscript(),
        session.getPartialContext(),
        session.getFinalUserContext(),
        session.getMissingRequiredSlotKeys(),
        session.getFollowUpQuestion(),
        session.getSelectedOutfitId(),
        session.getSimulationDraft(),
        session.getFeedbackResult(),
        session.getFinalResult());
  }

  public ClientSession toDomain() {
    return ClientSession.restore(
        sessionId,
        channel,
        status,
        contextStatus,
        followUpAttempt,
        briefingTranscript,
        partialContext,
        finalUserContext,
        missingRequiredSlotKeys,
        followUpQuestion,
        selectedOutfitId,
        simulationDraft,
        feedbackResult,
        finalResult);
  }
}
