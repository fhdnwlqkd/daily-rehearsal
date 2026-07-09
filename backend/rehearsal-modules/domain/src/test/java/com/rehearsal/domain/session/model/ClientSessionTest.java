package com.rehearsal.domain.session.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientSessionTest {

  private static final String FIRST_OPPONENT_LINE = "오는 길 괜찮으셨어요?";

  @Test
  void recordTurnIncrementsCurrentTurnOnSuccess() {
    ClientSession session = playingSessionAtTurn(1);

    session.recordTurn("네, 여유 있게 도착했어요.", true, "자연스럽습니다.", false);

    assertThat(session.getCurrentTurn()).isEqualTo(2);
    assertThat(session.getConversationHistory())
        .containsExactly(new ConversationHistory(1, FIRST_OPPONENT_LINE, "네, 여유 있게 도착했어요."));
    assertThat(session.getTurnEvaluations())
        .containsExactly(new TurnEvaluation(1, true, "자연스럽습니다.", false));
  }

  @Test
  void recordTurnKeepsCurrentTurnOnFailure() {
    ClientSession session = playingSessionAtTurn(1);

    session.recordTurn("...", false, "다시 시도해보세요.", true);

    assertThat(session.getCurrentTurn()).isEqualTo(1);
    assertThat(session.getConversationHistory()).hasSize(1);
    assertThat(session.getTurnEvaluations())
        .containsExactly(new TurnEvaluation(1, false, "다시 시도해보세요.", true));
  }

  @Test
  void updateOpponentLineReplacesCurrentOpponentLine() {
    ClientSession session = playingSessionAtTurn(1);

    session.updateOpponentLine("다음 발화입니다.");

    assertThat(session.getCurrentOpponentLine()).isEqualTo("다음 발화입니다.");
  }

  @Test
  void updateOpponentLineThrowsInvalidSessionStateWhenNotPlaying() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);

    assertThatThrownBy(() -> session.updateOpponentLine("다음 발화입니다."))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void recordTurnThrowsInvalidSessionStateWhenNotPlaying() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);

    assertThatThrownBy(() -> session.recordTurn("transcript", true, "feedback", false))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void startContextExtractionRequiresContextNotStarted() {
    ClientSession session =
        ClientSession.builder()
            .sessionId("test-session-id")
            .situationType(com.rehearsal.domain.situation.model.SituationType.DATE)
            .status(SessionStatus.BRIEFING)
            .contextStatus(ContextStatus.COMPLETED)
            .build();

    assertThatThrownBy(session::startContextExtraction)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void requireFollowUpStoresPartialContextAndMovesToFollowUpRequired() {
    ClientSession session =
        ClientSession.create(com.rehearsal.domain.situation.model.SituationType.DATE);
    session.startContextExtraction();

    session.requireFollowUp(
        Map.of("situation_type", "date"),
        java.util.List.of("critical_moment"),
        java.util.List.of("Which moment are you most worried about?"));

    assertThat(session.getStatus()).isEqualTo(SessionStatus.FOLLOW_UP_REQUIRED);
    assertThat(session.getContextStatus()).isEqualTo(ContextStatus.FOLLOW_UP_REQUIRED);
    assertThat(session.getPartialContext()).containsEntry("situation_type", "date");
    assertThat(session.getMissingSlotKeys()).containsExactly("critical_moment");
    assertThat(session.getFollowUpQuestions())
        .containsExactly("Which moment are you most worried about?");
  }

  @Test
  void completeContextStoresFinalContextAndMovesToTransformationReady() {
    ClientSession session =
        ClientSession.create(com.rehearsal.domain.situation.model.SituationType.DATE);
    session.startContextExtraction();

    session.completeContext(Map.of("situation_type", "date", "desired_persona", "warm_natural"));

    assertThat(session.getStatus()).isEqualTo(SessionStatus.TRANSFORMATION_READY);
    assertThat(session.getContextStatus()).isEqualTo(ContextStatus.COMPLETED);
    assertThat(session.getFinalContext())
        .containsEntry("situation_type", "date")
        .containsEntry("desired_persona", "warm_natural");
    assertThat(session.getMissingSlotKeys()).isEmpty();
    assertThat(session.getFollowUpQuestions()).isEmpty();
  }

  @Test
  void startFollowUpMergeMovesFollowUpRequiredToMergingAndIncreasesAttempt() {
    ClientSession session = followUpRequiredSession();

    session.startFollowUpMerge();

    assertThat(session.getStatus()).isEqualTo(SessionStatus.CONTEXT_EXTRACTING);
    assertThat(session.getContextStatus()).isEqualTo(ContextStatus.MERGING);
    assertThat(session.getFollowUpAttempt()).isEqualTo(1);
  }

  @Test
  void startFollowUpMergeThrowsInvalidSessionStateWhenFollowUpIsNotRequired() {
    ClientSession session =
        ClientSession.create(com.rehearsal.domain.situation.model.SituationType.DATE);

    assertThatThrownBy(session::startFollowUpMerge)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void completeContextAllowsMergingContextStatus() {
    ClientSession session = mergingSession();

    session.completeContext(Map.of("situation_type", "date", "critical_moment", "first greeting"));

    assertThat(session.getStatus()).isEqualTo(SessionStatus.TRANSFORMATION_READY);
    assertThat(session.getContextStatus()).isEqualTo(ContextStatus.COMPLETED);
    assertThat(session.getFinalContext()).containsEntry("critical_moment", "first greeting");
  }

  @Test
  void requireFollowUpAllowsMergingContextStatus() {
    ClientSession session = mergingSession();

    session.requireFollowUp(
        Map.of("situation_type", "date", "desired_persona", "warm_natural"),
        java.util.List.of("critical_moment"),
        java.util.List.of("Which moment should we focus on?"));

    assertThat(session.getStatus()).isEqualTo(SessionStatus.FOLLOW_UP_REQUIRED);
    assertThat(session.getContextStatus()).isEqualTo(ContextStatus.FOLLOW_UP_REQUIRED);
    assertThat(session.getPartialContext()).containsEntry("desired_persona", "warm_natural");
    assertThat(session.getMissingSlotKeys()).containsExactly("critical_moment");
  }

  @Test
  void recordTurnAllowsFinalTurnAtMaxTurn() {
    ClientSession session = playingSessionAtTurn(3, 3);

    session.recordTurn("마지막 답변", true, "잘하셨습니다.", false);

    assertThat(session.getCurrentTurn()).isEqualTo(4);
  }

  @Test
  void recordTurnThrowsTurnLimitExceededPastMaxTurn() {
    ClientSession session = playingSessionAtTurn(4, 3);

    assertThatThrownBy(() -> session.recordTurn("transcript", true, "feedback", false))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SIMULATION_TURN_LIMIT_EXCEEDED);
  }

  private ClientSession playingSessionAtTurn(int currentTurn) {
    return playingSessionAtTurn(currentTurn, currentTurn);
  }

  private ClientSession playingSessionAtTurn(int currentTurn, int maxTurn) {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(maxTurn, FIRST_OPPONENT_LINE);
    for (int i = 1; i < currentTurn; i++) {
      session.recordTurn("transcript-" + i, true, "feedback-" + i, false);
    }
    return session;
  }

  private ClientSession sessionWith(SessionStatus status) {
    return ClientSession.builder()
        .sessionId("test-session-id")
        .situationType(com.rehearsal.domain.situation.model.SituationType.DATE)
        .status(status)
        .contextStatus(ContextStatus.COMPLETED)
        .partialContext(Map.of())
        .finalContext(Map.of("situationType", "date"))
        .selectedOutfitId("test-outfit-id")
        .build();
  }

  private ClientSession followUpRequiredSession() {
    ClientSession session =
        ClientSession.create(com.rehearsal.domain.situation.model.SituationType.DATE);
    session.startContextExtraction();
    session.requireFollowUp(
        Map.of("situation_type", "date", "desired_persona", "warm_natural"),
        java.util.List.of("critical_moment"),
        java.util.List.of("Which moment should we focus on?"));
    return session;
  }

  private ClientSession mergingSession() {
    ClientSession session = followUpRequiredSession();
    session.startFollowUpMerge();
    return session;
  }
}
