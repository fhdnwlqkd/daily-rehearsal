package com.rehearsal.domain.session.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
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
  private int followUpAttempt;
  private Map<String, Object> partialContext;
  private Map<String, Object> finalContext;
  private List<String> missingSlotKeys;
  private List<String> followUpQuestions;
  private String selectedOutfitId;

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
      String selectedOutfitId) {
    this.sessionId = sessionId;
    this.situationType = situationType;
    this.status = status;
    this.contextStatus = contextStatus;
    this.followUpAttempt = followUpAttempt;
    this.partialContext = partialContext;
    this.finalContext = finalContext;
    this.missingSlotKeys = missingSlotKeys;
    this.followUpQuestions = followUpQuestions;
    this.selectedOutfitId = selectedOutfitId;
  }

  public static ClientSession create(SituationType situationType) {
    return new ClientSession(
        UUID.randomUUID().toString(),
        situationType,
        SessionStatus.BRIEFING,
        ContextStatus.NOT_STARTED,
        0,
        null,
        null,
        null,
        null,
        null);
  }

  public static ClientSession restore(
      String sessionId,
      SituationType situationType,
      SessionStatus status,
      ContextStatus contextStatus,
      int followUpAttempt,
      Map<String, Object> partialContext,
      Map<String, Object> finalContext,
      List<String> missingSlotKeys,
      List<String> followUpQuestions,
      String selectedOutfitId) {
    return new ClientSession(
        sessionId,
        situationType,
        status,
        contextStatus,
        followUpAttempt,
        partialContext,
        finalContext,
        missingSlotKeys,
        followUpQuestions,
        selectedOutfitId);
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

  private void validateStatus(SessionStatus expected) {
    if (status != expected) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }

  private void validateFinalContextCompleted() {
    if (contextStatus != ContextStatus.COMPLETED
        || finalContext == null
        || finalContext.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }
}
