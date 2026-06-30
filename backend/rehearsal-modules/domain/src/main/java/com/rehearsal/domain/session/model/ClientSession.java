package com.rehearsal.domain.session.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
@Description("P1 client rehearsal session state stored in Redis")
public class ClientSession {

  private String sessionId;
  private SituationType situationType;
  private SessionStatus status;
  private ContextStatus contextStatus;
  private int attempt;
  private String briefingTranscript;
  private Map<String, Object> partialContext;
  private Map<String, Object> finalContext;
  private List<String> missingSlotKeys;
  private List<String> followUpQuestions;
  private String selectedOutfitId;
  private int simulationTurn;
  private List<Map<String, Object>> conversationHistory;
  private List<Map<String, Object>> turnEvaluations;
  private String videoUrl;
  private Map<String, Object> ticket;
  private String downloadUrl;

  private ClientSession(
      String sessionId,
      SituationType situationType,
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
    this.sessionId = sessionId;
    this.situationType = situationType;
    this.status = status;
    this.contextStatus = contextStatus;
    this.attempt = attempt;
    this.briefingTranscript = briefingTranscript;
    this.partialContext = partialContext;
    this.finalContext = finalContext;
    this.missingSlotKeys = missingSlotKeys;
    this.followUpQuestions = followUpQuestions;
    this.selectedOutfitId = selectedOutfitId;
    this.simulationTurn = simulationTurn;
    this.conversationHistory = conversationHistory;
    this.turnEvaluations = turnEvaluations;
    this.videoUrl = videoUrl;
    this.ticket = ticket;
    this.downloadUrl = downloadUrl;
  }

  public static ClientSession create() {
    return new ClientSession(
        UUID.randomUUID().toString(),
        SituationType.DATE,
        SessionStatus.BRIEFING,
        ContextStatus.NOT_STARTED,
        0,
        null,
        null,
        null,
        null,
        null,
        null,
        0,
        List.of(),
        List.of(),
        null,
        null,
        null);
  }

  public static ClientSession restore(
      String sessionId,
      SituationType situationType,
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
    return new ClientSession(
        sessionId,
        situationType,
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

  public void updateStatus(SessionStatus status) {
    this.status = status;
  }

  public void updateContextStatus(ContextStatus contextStatus) {
    this.contextStatus = contextStatus;
  }

  public void updateBriefingTranscript(String briefingTranscript) {
    this.briefingTranscript = briefingTranscript;
  }

  public void updateSelectedOutfitId(String selectedOutfitId) {
    this.selectedOutfitId = selectedOutfitId;
  }
}
