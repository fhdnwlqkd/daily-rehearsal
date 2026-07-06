package com.rehearsal.domain.session.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Description("P1 client rehearsal session state stored in Redis")
public class ClientSession {

  private String sessionId;
  private SituationType situationType;
  private SessionStatus status;
  private ContextStatus contextStatus;
  private int followUpAttempt;
  private Map<String, Object> partialContext;
  private Map<String, Object> finalContext;
  private List<String> missingSlotKeys;
  private List<String> followUpQuestions;
  private String selectedOutfitId;
  private int currentTurn;
  private int maxTurn;
  private List<ConversationHistory> conversationHistory;
  private List<TurnEvaluation> turnEvaluations;

  private ClientSession(
      String sessionId,
      SituationType situationType,
      SessionStatus status,
      ContextStatus contextStatus,
      int followUpAttempt,
      Map<String, Object> partialContext,
      Map<String, Object> finalContext,
      List<String> missingSlotKeys,
      List<String> followUpQuestions,
      String selectedOutfitId,
      int currentTurn,
      int maxTurn,
      List<ConversationHistory> conversationHistory,
      List<TurnEvaluation> turnEvaluations) {
    this.sessionId = sessionId;
    this.situationType = situationType;
    this.status = status;
    this.contextStatus = contextStatus;
    this.followUpAttempt = followUpAttempt;
    this.partialContext = partialContext;
    this.finalContext = finalContext;
    this.missingSlotKeys = mutableList(missingSlotKeys);
    this.followUpQuestions = mutableList(followUpQuestions);
    this.selectedOutfitId = selectedOutfitId;
    this.currentTurn = currentTurn;
    this.maxTurn = maxTurn;
    this.conversationHistory = mutableList(conversationHistory);
    this.turnEvaluations = mutableList(turnEvaluations);
  }

  public static ClientSession create(SituationType situationType) {
    return restore(
        Snapshot.builder()
            .sessionId(UUID.randomUUID().toString())
            .situationType(situationType)
            .status(SessionStatus.BRIEFING)
            .contextStatus(ContextStatus.NOT_STARTED)
            .build());
  }

  public static ClientSession restore(Snapshot snapshot) {
    return new ClientSession(
        snapshot.sessionId(),
        snapshot.situationType(),
        snapshot.status(),
        snapshot.contextStatus(),
        snapshot.followUpAttempt(),
        snapshot.partialContext(),
        snapshot.finalContext(),
        snapshot.missingSlotKeys(),
        snapshot.followUpQuestions(),
        snapshot.selectedOutfitId(),
        snapshot.currentTurn(),
        snapshot.maxTurn(),
        snapshot.conversationHistory(),
        snapshot.turnEvaluations());
  }

  public void startContextExtraction() {
    validateStatus(SessionStatus.BRIEFING);
    this.status = SessionStatus.CONTEXT_EXTRACTING;
    this.contextStatus = ContextStatus.EXTRACTING;
  }

  public void selectOutfit(String selectedOutfitId) {
    validateStatus(SessionStatus.TRANSFORMATION_READY);
    this.selectedOutfitId = selectedOutfitId;
  }

  public void confirmOutfit(String selectedOutfitId) {
    validateStatus(SessionStatus.TRANSFORMATION_READY);
    validateFinalContextCompleted();
    this.selectedOutfitId = selectedOutfitId;
    this.status = SessionStatus.REHEARSAL_READY;
  }

  public void startSimulation(int maxTurn) {
    validateStatus(SessionStatus.REHEARSAL_READY);
    this.status = SessionStatus.REHEARSAL_PLAYING;
    this.currentTurn = 1;
    this.maxTurn = maxTurn;
  }

  public void recordTurn(
      String opponentLine,
      String userTranscript,
      boolean success,
      String feedback,
      boolean fallback) {
    validateStatus(SessionStatus.REHEARSAL_PLAYING);
    validateTurnLimit();
    conversationHistory.add(new ConversationHistory(currentTurn, opponentLine, userTranscript));
    turnEvaluations.add(new TurnEvaluation(currentTurn, success, feedback, fallback));
    if (success) {
      currentTurn++;
    }
  }

  public List<String> getMissingSlotKeys() {
    return List.copyOf(missingSlotKeys);
  }

  public List<String> getFollowUpQuestions() {
    return List.copyOf(followUpQuestions);
  }

  public List<ConversationHistory> getConversationHistory() {
    return List.copyOf(conversationHistory);
  }

  public List<TurnEvaluation> getTurnEvaluations() {
    return List.copyOf(turnEvaluations);
  }

  private void validateStatus(SessionStatus expected) {
    if (status != expected) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }

  private void validateTurnLimit() {
    if (currentTurn > maxTurn) {
      throw new BusinessException(ErrorCode.SIMULATION_TURN_LIMIT_EXCEEDED);
    }
  }

  private void validateFinalContextCompleted() {
    if (contextStatus != ContextStatus.COMPLETED
        || finalContext == null
        || finalContext.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }

  private static <T> List<T> mutableList(List<T> values) {
    return values == null ? new ArrayList<>() : new ArrayList<>(values);
  }

  @Builder
  public record Snapshot(
      String sessionId,
      SituationType situationType,
      SessionStatus status,
      ContextStatus contextStatus,
      int followUpAttempt,
      Map<String, Object> partialContext,
      Map<String, Object> finalContext,
      List<String> missingSlotKeys,
      List<String> followUpQuestions,
      String selectedOutfitId,
      int currentTurn,
      int maxTurn,
      List<ConversationHistory> conversationHistory,
      List<TurnEvaluation> turnEvaluations) {}
}
