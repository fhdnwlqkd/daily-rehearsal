package com.rehearsal.api.rehearsal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SimulationServiceTest {

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
    SimulationService service = new SimulationService(new EmptySessionCache());

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
    session.startSimulation(3);
    SimulationService service = serviceWith(session);

    service.recordTurnResult(
        session.getSessionId(), "오는 길 괜찮으셨어요?", "네, 여유 있게 도착했어요.", true, "자연스럽습니다.", false);

    assertThat(session.getCurrentTurn()).isEqualTo(2);
    assertThat(session.getConversationHistory()).hasSize(1);
    assertThat(session.getTurnEvaluations()).hasSize(1);
  }

  @Test
  void recordTurnResultThrowsSessionNotFound() {
    SimulationService service = new SimulationService(new EmptySessionCache());

    assertThatThrownBy(
            () ->
                service.recordTurnResult(
                    "unknown-session-id", "line", "transcript", true, "feedback", false))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
  }

  private SimulationService serviceWith(ClientSession session) {
    return new SimulationService(new InMemorySessionCache(session));
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
        java.util.List.of(),
        java.util.List.of(),
        "test-outfit-id",
        0,
        0,
        java.util.List.of(),
        java.util.List.of());
  }

  static class EmptySessionCache implements SessionCache {

    @Override
    public ClientSession save(ClientSession session) {
      return session;
    }

    @Override
    public Optional<ClientSession> findById(String sessionId) {
      return Optional.empty();
    }
  }

  static class InMemorySessionCache implements SessionCache {

    private final Map<String, ClientSession> store = new HashMap<>();

    InMemorySessionCache(ClientSession session) {
      store.put(session.getSessionId(), session);
    }

    @Override
    public ClientSession save(ClientSession session) {
      store.put(session.getSessionId(), session);
      return session;
    }

    @Override
    public Optional<ClientSession> findById(String sessionId) {
      return Optional.ofNullable(store.get(sessionId));
    }
  }
}
