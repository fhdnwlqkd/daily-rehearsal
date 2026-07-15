package com.rehearsal.domain.session.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.situation.model.SituationType;
import org.junit.jupiter.api.Test;

class ClientSessionTest {

  @Test
  void createStartsAtBriefing() {
    ClientSession session = ClientSession.create(SituationType.DATE);

    assertThat(session.getSessionId()).isNotBlank();
    assertThat(session.getStatus()).isEqualTo(SessionStatus.BRIEFING);
    assertThat(session.getContextStatus()).isEqualTo(ContextStatus.NOT_STARTED);
  }

  @Test
  void contextCanRequireFollowUpAndStartMerge() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    session.requireFollowUp();
    session.startFollowUpMerge();

    assertThat(session.getStatus()).isEqualTo(SessionStatus.CONTEXT_EXTRACTING);
    assertThat(session.getContextStatus()).isEqualTo(ContextStatus.MERGING);
    assertThat(session.getFollowUpAttempt()).isEqualTo(1);
  }

  @Test
  void contextCanCompleteFromInitialExtraction() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    session.completeContext();

    assertThat(session.getStatus()).isEqualTo(SessionStatus.TRANSFORMATION_READY);
    assertThat(session.getContextStatus()).isEqualTo(ContextStatus.COMPLETED);
  }

  @Test
  void contextCanFailWhileExtracting() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    session.failContext();

    assertThat(session.getStatus()).isEqualTo(SessionStatus.FAILED);
    assertThat(session.getContextStatus()).isEqualTo(ContextStatus.FAILED);
  }

  @Test
  void confirmOutfitRequiresCompletedContext() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    session.completeContext();
    session.confirmOutfit("outfit-1");

    assertThat(session.getStatus()).isEqualTo(SessionStatus.REHEARSAL_READY);
    assertThat(session.getSelectedOutfitId()).isEqualTo("outfit-1");
  }

  @Test
  void simulationStartsAndAdvancesTurn() {
    ClientSession session = rehearsalReadySession();
    session.startSimulation(3);
    session.advanceTurn();

    assertThat(session.getStatus()).isEqualTo(SessionStatus.REHEARSAL_PLAYING);
    assertThat(session.getCurrentTurn()).isEqualTo(2);
    assertThat(session.getMaxTurn()).isEqualTo(3);
  }

  @Test
  void invalidTransitionThrowsBusinessException() {
    ClientSession session = ClientSession.create(SituationType.DATE);

    assertThatThrownBy(session::completeContext)
        .isInstanceOf(BusinessException.class)
        .extracting(error -> ((BusinessException) error).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void advancingPastTurnLimitIsRejected() {
    ClientSession session = rehearsalReadySession();
    session.startSimulation(1);
    session.advanceTurn();

    assertThatThrownBy(session::advanceTurn)
        .isInstanceOf(BusinessException.class)
        .extracting(error -> ((BusinessException) error).getErrorCode())
        .isEqualTo(ErrorCode.SIMULATION_TURN_LIMIT_EXCEEDED);
  }

  private ClientSession rehearsalReadySession() {
    return ClientSession.builder()
        .sessionId("session-id")
        .situationType(SituationType.DATE)
        .status(SessionStatus.REHEARSAL_READY)
        .contextStatus(ContextStatus.COMPLETED)
        .selectedOutfitId("outfit-1")
        .build();
  }
}
