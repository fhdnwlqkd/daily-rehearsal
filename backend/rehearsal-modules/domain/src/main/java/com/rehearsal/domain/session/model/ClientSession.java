package com.rehearsal.domain.session.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
public class ClientSession {

  public static final String DEFAULT_CHANNEL = "P1_OFFLINE";

  private String sessionId;
  private String channel;
  private SessionStatus status;
  private ContextStatus contextStatus;
  private int followUpAttempt;
  private String briefingTranscript;
  private Map<String, Object> partialContext;
  private Map<String, Object> finalUserContext;
  private List<String> missingRequiredSlotKeys;
  private String followUpQuestion;
  private String selectedOutfitId;
  private Map<String, Object> simulationDraft;
  private Map<String, Object> feedbackResult;
  private Map<String, Object> finalResult;

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

  public void updateStatus(SessionStatus status) {
    this.status = status;
  }

  public void updateContextStatus(ContextStatus contextStatus) {
    this.contextStatus = contextStatus;
  }

  public void updateBriefingTranscript(String briefingTranscript) {
    this.briefingTranscript = briefingTranscript;
  }

  public void updateContext(
      Map<String, Object> partialContext,
      List<String> missingRequiredSlotKeys,
      String followUpQuestion) {
    this.partialContext = partialContext;
    this.missingRequiredSlotKeys = missingRequiredSlotKeys;
    this.followUpQuestion = followUpQuestion;
  }

  public void updateFinalUserContext(Map<String, Object> finalUserContext) {
    this.finalUserContext = finalUserContext;
    this.missingRequiredSlotKeys = List.of();
    this.followUpQuestion = null;
  }

  public void updateSelectedOutfitId(String selectedOutfitId) {
    this.selectedOutfitId = selectedOutfitId;
  }

  public void updateSimulationDraft(Map<String, Object> simulationDraft) {
    this.simulationDraft = simulationDraft;
  }

  public void updateFeedbackResult(Map<String, Object> feedbackResult) {
    this.feedbackResult = feedbackResult;
  }

  public void updateFinalResult(Map<String, Object> finalResult) {
    this.finalResult = finalResult;
  }

  public void increaseFollowUpAttempt() {
    this.followUpAttempt++;
  }

  private static String normalizeChannel(String channel) {
    if (channel == null || channel.isBlank()) {
      return DEFAULT_CHANNEL;
    }
    return channel;
  }
}
