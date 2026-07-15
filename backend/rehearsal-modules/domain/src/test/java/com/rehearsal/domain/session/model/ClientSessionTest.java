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

  @Test
  void newSessionHasNoneVideoUploadStatusByDefault() {
    ClientSession session = ClientSession.create(SituationType.DATE);

    assertThat(session.getVideoUploadStatus()).isEqualTo(VideoUploadStatus.NONE);
    assertThat(session.getVideoUrl()).isNull();
  }

  @Test
  void assignVideoUrlSetsUrlAndPendingStatus() {
    ClientSession session = playingSessionAtTurn(1);

    session.assignVideoUrl("http://localhost/mock-videos/test-session-id.webm");

    assertThat(session.getVideoUrl())
        .isEqualTo("http://localhost/mock-videos/test-session-id.webm");
    assertThat(session.getVideoUploadStatus()).isEqualTo(VideoUploadStatus.PENDING);
    assertThat(session.getVideoUploadFailureReason()).isNull();
  }

  @Test
  void completeVideoUploadMarksStatusCompleted() {
    ClientSession session = playingSessionAtTurn(1);
    session.assignVideoUrl("http://localhost/mock-videos/test-session-id.webm");

    session.completeVideoUpload();

    assertThat(session.getVideoUploadStatus()).isEqualTo(VideoUploadStatus.COMPLETED);
  }

  @Test
  void failVideoUploadRecordsFailureReason() {
    ClientSession session = playingSessionAtTurn(1);
    session.assignVideoUrl("http://localhost/mock-videos/test-session-id.webm");

    session.failVideoUpload("storage unavailable");

    assertThat(session.getVideoUploadStatus()).isEqualTo(VideoUploadStatus.FAILED);
    assertThat(session.getVideoUploadFailureReason()).isEqualTo("storage unavailable");
  }

  @Test
  void completeSimulationMovesPlayingSessionPastMaxTurnToCompleted() {
    ClientSession session = playingSessionAtTurn(4, 3);

    session.completeSimulation();

    assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
  }

  @Test
  void completeSimulationThrowsInvalidSessionStateWhenNotPlaying() {
    ClientSession session = rehearsalReadySession();

    assertThatThrownBy(session::completeSimulation)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void completeSimulationThrowsSimulationNotCompletedWhenTurnsRemain() {
    ClientSession session = playingSessionAtTurn(1, 3);

    assertThatThrownBy(session::completeSimulation)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SIMULATION_NOT_COMPLETED);
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

  private ClientSession playingSessionAtTurn(int currentTurn) {
    return playingSessionAtTurn(currentTurn, currentTurn);
  }

  private ClientSession playingSessionAtTurn(int currentTurn, int maxTurn) {
    ClientSession session = rehearsalReadySession();
    session.startSimulation(maxTurn);
    for (int i = 1; i < currentTurn; i++) {
      session.advanceTurn();
    }
    return session;
  }
}
