package com.rehearsal.domain.session.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Description("RDB-backed P1 rehearsal session aggregate root")
public class ClientSession {

  private String sessionId;
  private SituationType situationType;
  private SessionStatus status;
  private ContextStatus contextStatus;
  private int followUpAttempt;
  private String selectedOutfitId;
  private int currentTurn;
  private int maxTurn;

  @Builder
  private ClientSession(
      String sessionId,
      SituationType situationType,
      SessionStatus status,
      ContextStatus contextStatus,
      int followUpAttempt,
      String selectedOutfitId,
      int currentTurn,
      int maxTurn) {
    this.sessionId = sessionId;
    this.situationType = situationType;
    this.status = status;
    this.contextStatus = contextStatus;
    this.followUpAttempt = followUpAttempt;
    this.selectedOutfitId = selectedOutfitId;
    this.currentTurn = currentTurn;
    this.maxTurn = maxTurn;
  }

  public static ClientSession create(SituationType situationType) {
    return ClientSession.builder()
        .sessionId(UUID.randomUUID().toString())
        .situationType(situationType)
        .status(SessionStatus.BRIEFING)
        .contextStatus(ContextStatus.NOT_STARTED)
        .build();
  }

  public void startContextExtraction() {
    validateStatus(SessionStatus.BRIEFING);
    validateContextStatus(ContextStatus.NOT_STARTED);
    this.status = SessionStatus.CONTEXT_EXTRACTING;
    this.contextStatus = ContextStatus.EXTRACTING;
  }

  public void requireFollowUp() {
    validateStatus(SessionStatus.CONTEXT_EXTRACTING);
    validateContextStatusAny(ContextStatus.EXTRACTING, ContextStatus.MERGING);
    this.status = SessionStatus.FOLLOW_UP_REQUIRED;
    this.contextStatus = ContextStatus.FOLLOW_UP_REQUIRED;
  }

  public void completeContext() {
    validateStatus(SessionStatus.CONTEXT_EXTRACTING);
    validateContextStatusAny(ContextStatus.EXTRACTING, ContextStatus.MERGING);
    this.status = SessionStatus.TRANSFORMATION_READY;
    this.contextStatus = ContextStatus.COMPLETED;
  }

  public void startFollowUpMerge() {
    validateStatus(SessionStatus.FOLLOW_UP_REQUIRED);
    validateContextStatus(ContextStatus.FOLLOW_UP_REQUIRED);
    this.status = SessionStatus.CONTEXT_EXTRACTING;
    this.contextStatus = ContextStatus.MERGING;
    this.followUpAttempt++;
  }

  public void failContext() {
    validateStatus(SessionStatus.CONTEXT_EXTRACTING);
    validateContextStatusAny(ContextStatus.EXTRACTING, ContextStatus.MERGING);
    this.status = SessionStatus.FAILED;
    this.contextStatus = ContextStatus.FAILED;
  }

  public void selectOutfit(String selectedOutfitId) {
    validateStatus(SessionStatus.TRANSFORMATION_READY);
    this.selectedOutfitId = selectedOutfitId;
  }

  public void confirmOutfit(String selectedOutfitId) {
    validateStatus(SessionStatus.TRANSFORMATION_READY);
    validateContextCompleted();
    this.selectedOutfitId = selectedOutfitId;
    this.status = SessionStatus.REHEARSAL_READY;
  }

  public void startSimulation(int maxTurn) {
    validateStatus(SessionStatus.REHEARSAL_READY);
    this.status = SessionStatus.REHEARSAL_PLAYING;
    this.currentTurn = 1;
    this.maxTurn = maxTurn;
  }

  public void advanceTurn() {
    validateStatus(SessionStatus.REHEARSAL_PLAYING);
    validateTurnLimit();
    this.currentTurn++;
  }

  private void validateStatus(SessionStatus expected) {
    if (status != expected) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }

  private void validateContextStatus(ContextStatus expected) {
    if (contextStatus != expected) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }

  private void validateContextStatusAny(ContextStatus... expectedStatuses) {
    for (ContextStatus expected : expectedStatuses) {
      if (contextStatus == expected) {
        return;
      }
    }
    throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
  }

  private void validateTurnLimit() {
    if (currentTurn > maxTurn) {
      throw new BusinessException(ErrorCode.SIMULATION_TURN_LIMIT_EXCEEDED);
    }
  }

  private void validateContextCompleted() {
    if (contextStatus != ContextStatus.COMPLETED) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }
}
