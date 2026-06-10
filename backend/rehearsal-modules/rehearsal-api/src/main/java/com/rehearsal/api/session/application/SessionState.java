package com.rehearsal.api.session.application;

import com.rehearsal.api.session.contract.ContextStatus;
import com.rehearsal.api.session.contract.SessionStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SessionState {

  public static final String DEFAULT_CHANNEL = "P1_OFFLINE";

  private final String sessionId;
  private final String channel;
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

  private SessionState(String sessionId, String channel) {
    this.sessionId = sessionId;
    this.channel = normalizeChannel(channel);
    this.status = SessionStatus.BRIEFING;
    this.contextStatus = ContextStatus.NOT_STARTED;
    this.followUpAttempt = 0;
  }

  public static SessionState create(String channel) {
    return new SessionState(UUID.randomUUID().toString(), channel);
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getChannel() {
    return channel;
  }

  public SessionStatus getStatus() {
    return status;
  }

  public ContextStatus getContextStatus() {
    return contextStatus;
  }

  public int getFollowUpAttempt() {
    return followUpAttempt;
  }

  public String getBriefingTranscript() {
    return briefingTranscript;
  }

  public Map<String, Object> getPartialContext() {
    return partialContext;
  }

  public Map<String, Object> getFinalUserContext() {
    return finalUserContext;
  }

  public List<String> getMissingRequiredSlotKeys() {
    return missingRequiredSlotKeys;
  }

  public String getFollowUpQuestion() {
    return followUpQuestion;
  }

  public String getSelectedOutfitId() {
    return selectedOutfitId;
  }

  public Map<String, Object> getSimulationDraft() {
    return simulationDraft;
  }

  public Map<String, Object> getFeedbackResult() {
    return feedbackResult;
  }

  public Map<String, Object> getFinalResult() {
    return finalResult;
  }

  private static String normalizeChannel(String channel) {
    if (channel == null || channel.isBlank()) {
      return DEFAULT_CHANNEL;
    }
    return channel;
  }
}
