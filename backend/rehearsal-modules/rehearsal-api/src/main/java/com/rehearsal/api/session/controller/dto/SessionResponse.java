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

  public static SessionResponse from(ClientSession session) {
    return new SessionResponse(
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
}
