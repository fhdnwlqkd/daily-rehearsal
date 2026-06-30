package com.rehearsal.api.session.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionStatus;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Description("Current P1 client session state response")
public record SessionResponse(
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

  public static SessionResponse from(ClientSession session) {
    return new SessionResponse(
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
}
