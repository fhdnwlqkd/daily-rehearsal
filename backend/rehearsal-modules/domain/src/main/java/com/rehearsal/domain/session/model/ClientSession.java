package com.rehearsal.domain.session.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
public class ClientSession {

  public static final String DEFAULT_CHANNEL = "P1_OFFLINE";

  private final String sessionId;
  private final String channel;
  private final SessionStatus status;
  private final ContextStatus contextStatus;
  private final int followUpAttempt;
  private final String briefingTranscript;
  private final Map<String, Object> partialContext;
  private final Map<String, Object> finalUserContext;
  private final List<String> missingRequiredSlotKeys;
  private final String followUpQuestion;
  private final String selectedOutfitId;
  private final Map<String, Object> simulationDraft;
  private final Map<String, Object> feedbackResult;
  private final Map<String, Object> finalResult;

  private ClientSession(
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
    this.sessionId = sessionId;
    this.channel = normalizeChannel(channel);
    this.status = status;
    this.contextStatus = contextStatus;
    this.followUpAttempt = followUpAttempt;
    this.briefingTranscript = briefingTranscript;
    this.partialContext = partialContext;
    this.finalUserContext = finalUserContext;
    this.missingRequiredSlotKeys = missingRequiredSlotKeys;
    this.followUpQuestion = followUpQuestion;
    this.selectedOutfitId = selectedOutfitId;
    this.simulationDraft = simulationDraft;
    this.feedbackResult = feedbackResult;
    this.finalResult = finalResult;
  }

  public static ClientSession create(String channel) {
    return new ClientSession(
        UUID.randomUUID().toString(),
        channel,
        SessionStatus.BRIEFING,
        ContextStatus.NOT_STARTED,
        0,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public static ClientSession restore(
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
    return new ClientSession(
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

  public ClientSession updateBriefingTranscript(String briefingTranscript) {
    return copy(
        SessionStatus.CONTEXT_EXTRACTING,
        ContextStatus.EXTRACTING,
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

  public ClientSession updateContext(
      Map<String, Object> partialContext,
      List<String> missingRequiredSlotKeys,
      String followUpQuestion) {
    boolean followUpRequired =
        missingRequiredSlotKeys != null && !missingRequiredSlotKeys.isEmpty();
    return copy(
        followUpRequired ? SessionStatus.FOLLOW_UP_REQUIRED : status,
        followUpRequired ? ContextStatus.FOLLOW_UP_REQUIRED : ContextStatus.EXTRACTING,
        followUpRequired ? followUpAttempt + 1 : followUpAttempt,
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

  public ClientSession completeContext(Map<String, Object> finalUserContext) {
    return copy(
        SessionStatus.TRANSFORMATION_READY,
        ContextStatus.COMPLETED,
        followUpAttempt,
        briefingTranscript,
        partialContext,
        finalUserContext,
        List.of(),
        null,
        selectedOutfitId,
        simulationDraft,
        feedbackResult,
        finalResult);
  }

  public ClientSession updateSelectedOutfit(String selectedOutfitId) {
    return copy(
        SessionStatus.REHEARSAL_READY,
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

  public ClientSession updateSimulationDraft(Map<String, Object> simulationDraft) {
    return copy(
        SessionStatus.REHEARSAL_READY,
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

  public ClientSession updateFeedbackResult(Map<String, Object> feedbackResult) {
    return copy(
        SessionStatus.RESULT_READY,
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

  public ClientSession updateFinalResult(Map<String, Object> finalResult) {
    return copy(
        SessionStatus.COMPLETED,
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

  private static String normalizeChannel(String channel) {
    if (channel == null || channel.isBlank()) {
      return DEFAULT_CHANNEL;
    }
    return channel;
  }

  private ClientSession copy(
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
    return new ClientSession(
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
