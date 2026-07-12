package com.rehearsal.api.rehearsal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemorySessionRepository;
import com.rehearsal.domain.rehearsal.model.OpponentLineStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NextOpponentLineWorkerTest {

  @Test
  void completesPendingTurnWithGeneratedLine() {
    InMemorySessionRepository repository = repositoryWithPendingTurn();
    NextOpponentLineWorker worker = worker(repository, command -> "next line");

    worker.generate(new OpponentLineRequested("session-id", 2));

    SimulationTurn turn = repository.findTurn("session-id", 2).orElseThrow();
    assertThat(turn.getOpponentLineStatus()).isEqualTo(OpponentLineStatus.COMPLETED);
    assertThat(turn.getOpponentLine()).isEqualTo("next line");
  }

  @Test
  void AIErrorUsesConfiguredFallbackLine() {
    InMemorySessionRepository repository = repositoryWithPendingTurn();
    NextOpponentLineWorker worker =
        worker(repository, command -> { throw new IllegalStateException("AI down"); });

    worker.generate(new OpponentLineRequested("session-id", 2));

    SimulationTurn turn = repository.findTurn("session-id", 2).orElseThrow();
    assertThat(turn.getOpponentLineStatus()).isEqualTo(OpponentLineStatus.COMPLETED);
    assertThat(turn.getOpponentLine()).isNotBlank();
  }

  private NextOpponentLineWorker worker(
      InMemorySessionRepository repository,
      com.rehearsal.domain.rehearsal.port.OpponentLineGeneratorClient client) {
    return new NextOpponentLineWorker(
        new SessionReader(repository),
        repository,
        new SimulationContextReader(repository),
        client);
  }

  private InMemorySessionRepository repositoryWithPendingTurn() {
    ClientSession session =
        ClientSession.builder()
            .sessionId("session-id")
            .situationType(SituationType.DATE)
            .status(SessionStatus.REHEARSAL_PLAYING)
            .contextStatus(ContextStatus.COMPLETED)
            .selectedOutfitId("outfit-1")
            .currentTurn(2)
            .maxTurn(3)
            .build();
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    repository.saveContext(
        "session-id", SessionContext.from(SituationType.DATE, Map.of("desired_persona", "warm")));
    repository.saveTurn(SimulationTurn.pending("session-id", 2));
    return repository;
  }
}
