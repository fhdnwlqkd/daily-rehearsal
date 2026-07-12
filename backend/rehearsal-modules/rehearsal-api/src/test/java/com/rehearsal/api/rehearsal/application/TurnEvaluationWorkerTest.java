package com.rehearsal.api.rehearsal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemorySessionRepository;
import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;
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
    InMemorySessionRepository repository = repositoryWithPendingAttempt();
    TurnEvaluationWorker worker =
        worker(repository, command -> new TurnEvaluationRawResult(true, "good"));

    worker.evaluate(new TurnEvaluationRequested("session-id", 1, 1, null));

    SimulationTurn turn = repository.findTurn("session-id", 1).orElseThrow();
    SimulationTurnAttempt attempt = repository.findAttempt(turn.getId(), 1).orElseThrow();
    assertThat(attempt.getEvaluationStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    assertThat(attempt.getSuccess()).isTrue();
    assertThat(repository.findSession("session-id").orElseThrow().getCurrentTurn()).isEqualTo(2);
  }

  @Test
  void AIErrorUsesCompletedFallbackResult() {
    InMemorySessionRepository repository = repositoryWithPendingAttempt();
    TurnEvaluationWorker worker =
        worker(repository, command -> { throw new IllegalStateException("AI down"); });

    worker.evaluate(new TurnEvaluationRequested("session-id", 1, 1, null));

    SimulationTurn turn = repository.findTurn("session-id", 1).orElseThrow();
    SimulationTurnAttempt attempt = repository.findAttempt(turn.getId(), 1).orElseThrow();
    assertThat(attempt.getEvaluationStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    assertThat(attempt.getFallback()).isTrue();
    assertThat(attempt.getSuccess()).isFalse();
  }

  private TurnEvaluationWorker worker(
      InMemorySessionRepository repository,
      com.rehearsal.domain.rehearsal.port.TurnEvaluationClient client) {
    return new TurnEvaluationWorker(
        new SessionReader(repository),
        repository,
        new SimulationContextReader(repository),
        client);
  }

  private InMemorySessionRepository repositoryWithPendingAttempt() {
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
    SimulationTurn turn = repository.saveTurn(SimulationTurn.completed("session-id", 1, "hello"));
    repository.saveAttempt(SimulationTurnAttempt.pending(turn.getId(), 1, "answer"));
    return repository;
  }
}
