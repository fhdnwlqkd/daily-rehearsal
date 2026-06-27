package com.rehearsal.api.session.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.api.session.contract.ContextStatus;
import com.rehearsal.api.session.contract.SessionStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public final class SessionDto {

  private SessionDto() {}

  public record CreateSessionRequest(String channel) {}

  public record SubmitBriefingRequest(@NotBlank String transcript) {}

  public record SubmitFollowUpRequest(@NotBlank String transcript) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CreateSessionResponse(
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
      Map<String, Object> finalResult) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record GetSessionResponse(
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
      Map<String, Object> finalResult) {}
}
