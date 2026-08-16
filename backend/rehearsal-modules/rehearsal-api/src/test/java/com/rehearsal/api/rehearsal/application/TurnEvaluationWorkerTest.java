package com.rehearsal.api.rehearsal.application;

import static com.rehearsal.api.support.SimulationTestFixtures.completedTurn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemorySessionRepository;
import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationOutcome;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TurnEvaluationWorkerTest {

  @Test
  void completesAttemptAndAdvancesSessionOnSuccess() {
    InMemorySessionRepository repository = repositoryWithPendingAttempt(1);
    TurnEvaluationWorker worker =
        worker(repository, command -> new TurnEvaluationRawResult(true, "good"));

    worker.evaluate(new TurnEvaluationRequested("session-id", 1, 1, null));

    SimulationTurn turn = repository.findTurn("session-id", 1).orElseThrow();
    SimulationTurnAttempt attempt = repository.findAttempt(turn.getId(), 1).orElseThrow();
    assertThat(attempt.getEvaluationStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    assertThat(attempt.getOutcome()).isEqualTo(TurnEvaluationOutcome.ACCEPTED);
    assertThat(repository.findSession("session-id").orElseThrow().getCurrentTurn()).isEqualTo(2);
  }

  @Test
  void firstFailureCompletesAttemptWithoutAdvancingSession() {
    InMemorySessionRepository repository = repositoryWithPendingAttempt(1);
    TurnEvaluationWorker worker =
        worker(repository, command -> new TurnEvaluationRawResult(false, "try again"));

    worker.evaluate(new TurnEvaluationRequested("session-id", 1, 1, null));

    SimulationTurn turn = repository.findTurn("session-id", 1).orElseThrow();
    SimulationTurnAttempt attempt = repository.findAttempt(turn.getId(), 1).orElseThrow();
    assertThat(attempt.getOutcome()).isEqualTo(TurnEvaluationOutcome.RETRY_REQUIRED);
    assertThat(attempt.getFeedback()).isEqualTo("try again");
    assertThat(repository.findSession("session-id").orElseThrow().getCurrentTurn()).isEqualTo(1);
  }

  @Test
  void secondFailureKeepsFailureResultAndAdvancesSession() {
    InMemorySessionRepository repository = repositoryWithPendingAttempt(2);
    TurnEvaluationWorker worker =
        worker(repository, command -> new TurnEvaluationRawResult(false, "try again"));

    worker.evaluate(new TurnEvaluationRequested("session-id", 1, 2, null));

    SimulationTurn turn = repository.findTurn("session-id", 1).orElseThrow();
    SimulationTurnAttempt attempt = repository.findAttempt(turn.getId(), 2).orElseThrow();
    assertThat(attempt.getOutcome()).isEqualTo(TurnEvaluationOutcome.FORCED_ADVANCE);
    assertThat(attempt.getFallback()).isFalse();
    assertThat(attempt.getFeedback()).isEqualTo("try again 두 번의 연습을 마쳤어요. 다음 단계로 넘어갈게요.");
    assertThat(repository.findSession("session-id").orElseThrow().getCurrentTurn()).isEqualTo(2);
  }

  @Test
  void AIErrorKeepsFailureFallbackAndAdvancesSession() {
    InMemorySessionRepository repository = repositoryWithPendingAttempt(1);
    TurnEvaluationWorker worker =
        worker(
            repository,
            command -> {
              throw new IllegalStateException("AI down");
            });

    worker.evaluate(new TurnEvaluationRequested("session-id", 1, 1, null));

    SimulationTurn turn = repository.findTurn("session-id", 1).orElseThrow();
    SimulationTurnAttempt attempt = repository.findAttempt(turn.getId(), 1).orElseThrow();
    assertThat(attempt.getEvaluationStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    assertThat(attempt.getFallback()).isTrue();
    assertThat(attempt.getOutcome()).isEqualTo(TurnEvaluationOutcome.FORCED_ADVANCE);
    assertThat(attempt.getFeedback()).isEqualTo("답변을 확인했습니다. 다음 단계로 진행할게요.");
    assertThat(repository.findSession("session-id").orElseThrow().getCurrentTurn()).isEqualTo(2);
  }

  @Test
  void firstWorkerFailureLeavesOneRetry() {
    InMemorySessionRepository repository = repositoryWithPendingAttempt(1);
    SimulationContextReader contextReader = mock(SimulationContextReader.class);
    given(contextReader.context(any(ClientSession.class)))
        .willThrow(new IllegalStateException("worker failed"));
    TurnEvaluationWorker worker =
        worker(repository, contextReader, command -> new TurnEvaluationRawResult(true, "unused"));

    worker.evaluate(new TurnEvaluationRequested("session-id", 1, 1, null));

    SimulationTurn turn = repository.findTurn("session-id", 1).orElseThrow();
    SimulationTurnAttempt attempt = repository.findAttempt(turn.getId(), 1).orElseThrow();
    assertThat(attempt.getEvaluationStatus()).isEqualTo(EvaluationStatus.FAILED);
    assertThat(attempt.canRetry()).isTrue();
    assertThat(repository.findSession("session-id").orElseThrow().getCurrentTurn()).isEqualTo(1);
  }

  @Test
  void secondWorkerFailureForcesAdvanceWithoutOpeningAnotherRetry() {
    InMemorySessionRepository repository = repositoryWithPendingAttempt(2);
    SimulationContextReader contextReader = mock(SimulationContextReader.class);
    given(contextReader.context(any(ClientSession.class)))
        .willThrow(new IllegalStateException("worker failed"));
    TurnEvaluationWorker worker =
        worker(repository, contextReader, command -> new TurnEvaluationRawResult(true, "unused"));

    worker.evaluate(new TurnEvaluationRequested("session-id", 1, 2, null));

    SimulationTurn turn = repository.findTurn("session-id", 1).orElseThrow();
    SimulationTurnAttempt attempt = repository.findAttempt(turn.getId(), 2).orElseThrow();
    assertThat(attempt.getEvaluationStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    assertThat(attempt.getOutcome()).isEqualTo(TurnEvaluationOutcome.FORCED_ADVANCE);
    assertThat(attempt.getFallback()).isTrue();
    assertThat(attempt.getFeedback()).isEqualTo("두 번의 연습을 마쳤어요. 다음 단계로 넘어갈게요.");
    assertThat(attempt.canRetry()).isFalse();
    assertThat(repository.findSession("session-id").orElseThrow().getCurrentTurn()).isEqualTo(2);
  }

  @Test
  void secondRejectedAttemptForcesAdvanceWithoutAddingFailedAnswerToHistory() {
    InMemorySessionRepository repository = repositoryWithPendingAttempt(1);
    SimulationTurn turn = repository.findTurn("session-id", 1).orElseThrow();
    SimulationTurnAttempt first = repository.findAttempt(turn.getId(), 1).orElseThrow();
    first.complete(new TurnEvaluationResult(TurnEvaluationOutcome.RETRY_REQUIRED, "retry", false));
    repository.saveAttempt(first);
    repository.saveAttempt(SimulationTurnAttempt.pending(turn.getId(), 2, "still off topic"));
    TurnEvaluationWorker worker =
        worker(repository, command -> new TurnEvaluationRawResult(false, "next turn"));

    worker.evaluate(new TurnEvaluationRequested("session-id", 1, 2, null));

    SimulationTurnAttempt second = repository.findAttempt(turn.getId(), 2).orElseThrow();
    assertThat(second.getOutcome()).isEqualTo(TurnEvaluationOutcome.FORCED_ADVANCE);
    assertThat(repository.findSession("session-id").orElseThrow().getCurrentTurn()).isEqualTo(2);
    assertThat(new SimulationContextReader(repository).history("session-id", 2)).isEmpty();
  }

  private TurnEvaluationWorker worker(
      InMemorySessionRepository repository,
      com.rehearsal.domain.rehearsal.port.TurnEvaluationClient client) {
    return worker(repository, new SimulationContextReader(repository), client);
  }

  private TurnEvaluationWorker worker(
      InMemorySessionRepository repository,
      SimulationContextReader contextReader,
      com.rehearsal.domain.rehearsal.port.TurnEvaluationClient client) {
    return new TurnEvaluationWorker(
        new SessionReader(repository), repository, contextReader, client);
  }

  private InMemorySessionRepository repositoryWithPendingAttempt(int attemptNo) {
    ClientSession session =
        ClientSession.builder()
            .sessionId("session-id")
            .situationType(SituationType.DATE)
            .status(SessionStatus.REHEARSAL_PLAYING)
            .contextStatus(ContextStatus.COMPLETED)
            .selectedOutfitId("outfit-1")
            .currentTurn(1)
            .maxTurn(3)
            .build();
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    repository.saveContext(
        "session-id", SessionContext.from(SituationType.DATE, Map.of("desired_persona", "warm")));
    SimulationTurn turn = repository.saveTurn(completedTurn("session-id", 1, "hello"));
    repository.saveAttempt(SimulationTurnAttempt.pending(turn.getId(), attemptNo, "answer"));
    return repository;
  }
}
