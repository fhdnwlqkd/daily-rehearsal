package com.rehearsal.api.rehearsal.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.OpponentLineStatus;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.usecase.GetNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.GetTurnEvaluationUseCase;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitTurnEvaluationUseCase;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.repository.SessionRepository;
import com.rehearsal.domain.situation.model.SituationType;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class SimulationPollingIntegrationTest {

  private static final Duration POLLING_TIMEOUT = Duration.ofSeconds(5);

  @Autowired private SessionRepository sessionRepository;
  @Autowired private StartSimulationUseCase startSimulationUseCase;
  @Autowired private SubmitTurnEvaluationUseCase submitTurnEvaluationUseCase;
  @Autowired private GetTurnEvaluationUseCase getTurnEvaluationUseCase;
  @Autowired private SubmitNextOpponentLineUseCase submitNextOpponentLineUseCase;
  @Autowired private GetNextOpponentLineUseCase getNextOpponentLineUseCase;

  @Test
  void evaluationAndNextLineCompleteThroughRdbPolling() throws InterruptedException {
    ClientSession session = rehearsalReadySession();

    SimulationStart started = startSimulationUseCase.startSimulation(session.getSessionId());
    assertThat(started.currentTurn()).isEqualTo(1);

    SimulationTurnAttempt submitted =
        submitTurnEvaluationUseCase.submit(
            session.getSessionId(), 1, "Nice to meet you too.", null);
    assertThat(submitted.getEvaluationStatus()).isEqualTo(EvaluationStatus.PENDING);

    SimulationTurnAttempt completedEvaluation =
        awaitEvaluationStatus(session.getSessionId(), 1, EvaluationStatus.COMPLETED);
    assertThat(completedEvaluation.getSuccess()).isTrue();
    assertThat(completedEvaluation.getFeedback()).isNotBlank();
    assertThat(sessionRepository.findSession(session.getSessionId()).orElseThrow().getCurrentTurn())
        .isEqualTo(2);

    SimulationTurn submittedNextLine =
        submitNextOpponentLineUseCase.submitNextLine(session.getSessionId(), 2);
    assertThat(submittedNextLine.getOpponentLineStatus()).isEqualTo(OpponentLineStatus.PENDING);

    SimulationTurn completedNextLine =
        awaitOpponentLineStatus(session.getSessionId(), 2, OpponentLineStatus.COMPLETED);
    assertThat(completedNextLine.getPlan().opponentLine()).isNotBlank();
  }

  private ClientSession rehearsalReadySession() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    session.completeContext();
    session.confirmOutfit("outfit-1");
    sessionRepository.saveSession(session);
    sessionRepository.saveContext(
        session.getSessionId(),
        SessionContext.from(
            SituationType.DATE,
            Map.of("desired_persona", "warm_natural", "critical_moment", "first greeting")));
    return session;
  }

  private SimulationTurnAttempt awaitEvaluationStatus(
      String sessionId, int turnNo, EvaluationStatus expectedStatus) throws InterruptedException {
    Instant deadline = Instant.now().plus(POLLING_TIMEOUT);
    SimulationTurnAttempt attempt = getTurnEvaluationUseCase.get(sessionId, turnNo);

    while (Instant.now().isBefore(deadline)) {
      if (attempt.getEvaluationStatus() == expectedStatus) {
        return attempt;
      }
      Thread.sleep(50);
      attempt = getTurnEvaluationUseCase.get(sessionId, turnNo);
    }

    throw new AssertionError(
        "Expected evaluation status "
            + expectedStatus
            + " but was "
            + attempt.getEvaluationStatus());
  }

  private SimulationTurn awaitOpponentLineStatus(
      String sessionId, int turnNo, OpponentLineStatus expectedStatus) throws InterruptedException {
    Instant deadline = Instant.now().plus(POLLING_TIMEOUT);
    SimulationTurn turn = getNextOpponentLineUseCase.getNextLine(sessionId, turnNo);

    while (Instant.now().isBefore(deadline)) {
      if (turn.getOpponentLineStatus() == expectedStatus) {
        return turn;
      }
      Thread.sleep(50);
      turn = getNextOpponentLineUseCase.getNextLine(sessionId, turnNo);
    }

    throw new AssertionError(
        "Expected opponent line status "
            + expectedStatus
            + " but was "
            + turn.getOpponentLineStatus());
  }
}
