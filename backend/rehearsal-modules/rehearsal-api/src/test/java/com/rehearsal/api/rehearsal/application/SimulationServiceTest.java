package com.rehearsal.api.rehearsal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemorySessionCache;
import com.rehearsal.api.support.TestClientSessions;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationClient;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import org.junit.jupiter.api.Test;

class SimulationServiceTest {

  private static final String FIRST_OPPONENT_LINE = "오는 길 괜찮으셨어요?";

  @Test
  void startsSimulationForReadySession() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    SimulationService service = serviceWith(session);

    SimulationStart result = service.startSimulation(session.getSessionId());

    assertThat(result.sessionId()).isEqualTo(session.getSessionId());
    assertThat(result.currentTurn()).isEqualTo(1);
    assertThat(result.maxTurn()).isEqualTo(3);
    assertThat(result.opponentLine()).isNotBlank();
    assertThat(session.getStatus()).isEqualTo(SessionStatus.REHEARSAL_PLAYING);
    assertThat(session.getCurrentTurn()).isEqualTo(1);
  }

  @Test
  void startSimulationThrowsSessionNotFound() {
    SimulationService service = serviceWith(new InMemorySessionCache());

    assertThatThrownBy(() -> service.startSimulation("unknown-session-id"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
  }

  @Test
  void startSimulationThrowsInvalidSessionStateWhenNotRehearsalReady() {
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    SimulationService service = serviceWith(session);

    assertThatThrownBy(() -> service.startSimulation(session.getSessionId()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void recordTurnResultPersistsHistoryAndAdvancesTurnOnSuccess() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    SimulationService service = serviceWith(session);

    service.recordTurnResult(session.getSessionId(), "네, 여유 있게 도착했어요.", true, "자연스럽습니다.", false);

    assertThat(session.getCurrentTurn()).isEqualTo(2);
    assertThat(session.getConversationHistory()).hasSize(1);
    assertThat(session.getTurnEvaluations()).hasSize(1);
  }

  @Test
  void recordTurnResultThrowsSessionNotFound() {
    SimulationService service = serviceWith(new InMemorySessionCache());

    assertThatThrownBy(
            () ->
                service.recordTurnResult(
                    "unknown-session-id", "transcript", true, "feedback", false))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
  }

  @Test
  void evaluateTurnPersistsSuccessResultAndAdvancesTurn() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session),
            command -> new TurnEvaluationRawResult(true, "자연스럽습니다."));

    TurnEvaluationResult result =
        service.evaluateTurn(session.getSessionId(), 1, "네, 여유 있게 도착했어요.", null);

    assertThat(result.success()).isTrue();
    assertThat(result.feedback()).isEqualTo("자연스럽습니다.");
    assertThat(result.fallback()).isFalse();
    assertThat(session.getCurrentTurn()).isEqualTo(2);
    assertThat(session.getConversationHistory()).hasSize(1);
    assertThat(session.getTurnEvaluations()).hasSize(1);
  }

  @Test
  void evaluateTurnFallsBackWhenAiCallFails() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session),
            command -> {
              throw new IllegalStateException("Gemini timeout");
            });

    TurnEvaluationResult result = service.evaluateTurn(session.getSessionId(), 1, "...", null);

    assertThat(result.success()).isFalse();
    assertThat(result.feedback()).isEqualTo("다시 시도해보세요.");
    assertThat(result.fallback()).isTrue();
    assertThat(session.getCurrentTurn()).isEqualTo(1);
  }

  @Test
  void evaluateTurnThrowsTurnMismatchWhenTurnNoDoesNotMatchCurrentTurn() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session),
            command -> new TurnEvaluationRawResult(true, "자연스럽습니다."));

    assertThatThrownBy(() -> service.evaluateTurn(session.getSessionId(), 2, "transcript", null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SIMULATION_TURN_MISMATCH);
  }

  private SimulationService serviceWith(ClientSession session) {
    return serviceWith(new InMemorySessionCache(session), failingTurnEvaluationClient());
  }

  private SimulationService serviceWith(InMemorySessionCache sessionCache) {
    return serviceWith(sessionCache, failingTurnEvaluationClient());
  }

  private SimulationService serviceWith(
      InMemorySessionCache sessionCache, TurnEvaluationClient turnEvaluationClient) {
    return new SimulationService(
        sessionCache, new SessionReader(sessionCache), turnEvaluationClient);
  }

  private TurnEvaluationClient failingTurnEvaluationClient() {
    return command -> {
      throw new UnsupportedOperationException("not used by this test");
    };
  }

  private ClientSession sessionWith(SessionStatus status) {
    return TestClientSessions.sessionWith(status);
  }
}
