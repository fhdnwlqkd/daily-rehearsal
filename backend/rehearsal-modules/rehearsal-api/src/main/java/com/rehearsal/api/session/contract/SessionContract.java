package com.rehearsal.api.session.contract;

import com.rehearsal.api.session.application.SessionState;
import java.util.List;
import java.util.Map;

public final class SessionContract {

  private SessionContract() {}

  public record CreateSessionCommand(String channel) {}

  public record CreateSessionResult(
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

    public static CreateSessionResult from(SessionState sessionState) {
      return new CreateSessionResult(
          sessionState.getSessionId(),
          sessionState.getChannel(),
          sessionState.getStatus(),
          sessionState.getContextStatus(),
          sessionState.getFollowUpAttempt(),
          sessionState.getBriefingTranscript(),
          sessionState.getPartialContext(),
          sessionState.getFinalUserContext(),
          sessionState.getMissingRequiredSlotKeys(),
          sessionState.getFollowUpQuestion(),
          sessionState.getSelectedOutfitId(),
          sessionState.getSimulationDraft(),
          sessionState.getFeedbackResult(),
          sessionState.getFinalResult());
    }
  }

  public record GetSessionResult(
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

    public static GetSessionResult from(SessionState sessionState) {
      return new GetSessionResult(
          sessionState.getSessionId(),
          sessionState.getChannel(),
          sessionState.getStatus(),
          sessionState.getContextStatus(),
          sessionState.getFollowUpAttempt(),
          sessionState.getBriefingTranscript(),
          sessionState.getPartialContext(),
          sessionState.getFinalUserContext(),
          sessionState.getMissingRequiredSlotKeys(),
          sessionState.getFollowUpQuestion(),
          sessionState.getSelectedOutfitId(),
          sessionState.getSimulationDraft(),
          sessionState.getFeedbackResult(),
          sessionState.getFinalResult());
    }
  }
}
