package com.rehearsal.domain.session.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientSessionTest {

  @Test
  void recordTurnIncrementsCurrentTurnOnSuccess() {
    ClientSession session = playingSessionAtTurn(1);

    session.recordTurn("오는 길 괜찮으셨어요?", "네, 여유 있게 도착했어요.", true, "자연스럽습니다.", false);

    assertThat(session.getCurrentTurn()).isEqualTo(2);
    assertThat(session.getConversationHistory())
        .containsExactly(new ConversationHistory(1, "오는 길 괜찮으셨어요?", "네, 여유 있게 도착했어요."));
    assertThat(session.getTurnEvaluations())
        .containsExactly(new TurnEvaluation(1, true, "자연스럽습니다.", false));
  }

  @Test
  void recordTurnKeepsCurrentTurnOnFailure() {
    ClientSession session = playingSessionAtTurn(1);

    session.recordTurn("오는 길 괜찮으셨어요?", "...", false, "다시 시도해보세요.", true);

    assertThat(session.getCurrentTurn()).isEqualTo(1);
    assertThat(session.getConversationHistory()).hasSize(1);
    assertThat(session.getTurnEvaluations())
        .containsExactly(new TurnEvaluation(1, false, "다시 시도해보세요.", true));
  }

  @Test
  void recordTurnThrowsInvalidSessionStateWhenNotPlaying() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);

    assertThatThrownBy(() -> session.recordTurn("line", "transcript", true, "feedback", false))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void recordTurnAllowsFinalTurnAtMaxTurn() {
    ClientSession session = playingSessionAtTurn(3, 3);

    session.recordTurn("마지막 발화", "마지막 답변", true, "잘하셨습니다.", false);

    assertThat(session.getCurrentTurn()).isEqualTo(4);
  }

  @Test
  void recordTurnThrowsTurnLimitExceededPastMaxTurn() {
    ClientSession session = playingSessionAtTurn(4, 3);

    assertThatThrownBy(() -> session.recordTurn("line", "transcript", true, "feedback", false))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SIMULATION_TURN_LIMIT_EXCEEDED);
  }

  private ClientSession playingSessionAtTurn(int currentTurn) {
    return playingSessionAtTurn(currentTurn, currentTurn);
  }

  private ClientSession playingSessionAtTurn(int currentTurn, int maxTurn) {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(maxTurn);
    for (int i = 1; i < currentTurn; i++) {
      session.recordTurn("line-" + i, "transcript-" + i, true, "feedback-" + i, false);
    }
    return session;
  }

  private ClientSession sessionWith(SessionStatus status) {
    return ClientSession.restore(
        "test-session-id",
        SituationType.DATE,
        status,
        ContextStatus.COMPLETED,
        0,
        Map.of(),
        Map.of(),
        List.of(),
        List.of(),
        "test-outfit-id",
        0,
        0,
        List.of(),
        List.of());
  }
}
