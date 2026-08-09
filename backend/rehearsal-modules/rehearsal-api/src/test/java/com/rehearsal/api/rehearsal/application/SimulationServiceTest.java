package com.rehearsal.api.rehearsal.application;

import static com.rehearsal.api.support.SimulationTestFixtures.completedTurn;
import static com.rehearsal.api.support.SimulationTestFixtures.pendingTurn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemorySessionRepository;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.OpponentLineStatus;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationOutcome;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.rehearsal.model.TurnGenerationMode;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationServiceTest {

  @Test
  void startSimulationPersistsSessionAndFirstTurn() {
    ClientSession session = readySession();
    InMemorySessionRepository repository = new InMemorySessionRepository(session);

    SimulationStart result =
        service(repository, new ArrayList<>()).startSimulation(session.getSessionId());

    assertThat(result.currentTurn()).isEqualTo(1);
    assertThat(repository.findSession(session.getSessionId()).orElseThrow().getStatus())
        .isEqualTo(SessionStatus.REHEARSAL_PLAYING);
    assertThat(repository.findTurn(session.getSessionId(), 1).orElseThrow().getOpponentLineStatus())
        .isEqualTo(OpponentLineStatus.COMPLETED);
  }

  @Test
  void submitEvaluationPersistsAttemptAndPublishesAfterCommitRequest() {
    ClientSession session = playingSession(1);
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    SimulationTurn turn = repository.saveTurn(completedTurn(session.getSessionId(), 1, "hello"));
    List<Object> events = new ArrayList<>();

    SimulationTurnAttempt attempt =
        service(repository, events).submit(session.getSessionId(), 1, "answer", null);

    assertThat(attempt.getSimulationTurnId()).isEqualTo(turn.getId());
    assertThat(attempt.getEvaluationStatus()).isEqualTo(EvaluationStatus.PENDING);
    assertThat(events)
        .containsExactly(new TurnEvaluationRequested(session.getSessionId(), 1, 1, null));
  }

  @Test
  void duplicateEvaluationSubmissionReusesPendingAttempt() {
    ClientSession session = playingSession(1);
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    SimulationTurn turn = repository.saveTurn(completedTurn(session.getSessionId(), 1, "hello"));
    SimulationTurnAttempt pending =
        repository.saveAttempt(SimulationTurnAttempt.pending(turn.getId(), 1, "answer"));
    List<Object> events = new ArrayList<>();

    SimulationTurnAttempt duplicated =
        service(repository, events).submit(session.getSessionId(), 1, "answer", null);

    assertThat(duplicated.getId()).isEqualTo(pending.getId());
    assertThat(events).isEmpty();
  }

  @Test
  void failedEvaluationIsRetriedAsNextAttempt() {
    ClientSession session = playingSession(1);
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    SimulationTurn turn = repository.saveTurn(completedTurn(session.getSessionId(), 1, "hello"));
    SimulationTurnAttempt failed =
        repository.saveAttempt(SimulationTurnAttempt.pending(turn.getId(), 1, "first"));
    failed.fail("failed");
    repository.saveAttempt(failed);

    SimulationTurnAttempt retried =
        service(repository, new ArrayList<>()).submit(session.getSessionId(), 1, "retry", null);

    assertThat(retried.getAttemptNo()).isEqualTo(2);
    assertThat(retried.getEvaluationStatus()).isEqualTo(EvaluationStatus.PENDING);
  }

  @Test
  void retryRequiredEvaluationCreatesSecondAttempt() {
    ClientSession session = playingSession(1);
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    SimulationTurn turn = repository.saveTurn(completedTurn(session.getSessionId(), 1, "hello"));
    SimulationTurnAttempt first =
        repository.saveAttempt(SimulationTurnAttempt.pending(turn.getId(), 1, "off topic"));
    first.complete(
        new TurnEvaluationResult(TurnEvaluationOutcome.RETRY_REQUIRED, "질문에 맞게 다시 답해보세요.", false));
    repository.saveAttempt(first);

    SimulationTurnAttempt retried =
        service(repository, new ArrayList<>()).submit(session.getSessionId(), 1, "retry", null);

    assertThat(retried.getAttemptNo()).isEqualTo(2);
    assertThat(retried.getEvaluationStatus()).isEqualTo(EvaluationStatus.PENDING);
  }

  @Test
  void submitNextLinePersistsPendingTurn() {
    ClientSession session = playingSession(2);
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    List<Object> events = new ArrayList<>();

    SimulationTurn turn = service(repository, events).submitNextLine(session.getSessionId(), 2);

    assertThat(turn.getOpponentLineStatus()).isEqualTo(OpponentLineStatus.PENDING);
    assertThat(turn.getGenerationMode()).isEqualTo(TurnGenerationMode.NORMAL);
    assertThat(events).containsExactly(new OpponentLineRequested(session.getSessionId(), 2));
  }

  @Test
  void forcedAdvanceGeneratesNextTurnInRecoveryMode() {
    ClientSession session = playingSession(2);
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    SimulationTurn previous =
        repository.saveTurn(completedTurn(session.getSessionId(), 1, "first line"));
    SimulationTurnAttempt attempt =
        repository.saveAttempt(SimulationTurnAttempt.pending(previous.getId(), 2, "off topic"));
    attempt.complete(
        new TurnEvaluationResult(TurnEvaluationOutcome.FORCED_ADVANCE, "move on", false));
    repository.saveAttempt(attempt);

    SimulationTurn turn =
        service(repository, new ArrayList<>()).submitNextLine(session.getSessionId(), 2);

    assertThat(turn.getGenerationMode()).isEqualTo(TurnGenerationMode.RECOVERY);
  }

  @Test
  void duplicateNextLineSubmissionReusesPendingTurn() {
    ClientSession session = playingSession(2);
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    SimulationTurn pending = repository.saveTurn(pendingTurn(session.getSessionId(), 2));
    List<Object> events = new ArrayList<>();

    SimulationTurn duplicated =
        service(repository, events).submitNextLine(session.getSessionId(), 2);

    assertThat(duplicated.getId()).isEqualTo(pending.getId());
    assertThat(events).isEmpty();
  }

  @Test
  void rejectsEvaluationForDifferentCurrentTurn() {
    ClientSession session = playingSession(1);
    InMemorySessionRepository repository = new InMemorySessionRepository(session);

    assertThatThrownBy(
            () ->
                service(repository, new ArrayList<>())
                    .submit(session.getSessionId(), 2, "answer", null))
        .isInstanceOf(BusinessException.class);
  }

  private SimulationService service(InMemorySessionRepository repository, List<Object> events) {
    return new SimulationService(repository, new SessionReader(repository), events::add);
  }

  private ClientSession readySession() {
    return ClientSession.builder()
        .sessionId("session-id")
        .situationType(SituationType.DATE)
        .status(SessionStatus.REHEARSAL_READY)
        .contextStatus(ContextStatus.COMPLETED)
        .selectedOutfitId("outfit-1")
        .build();
  }

  private ClientSession playingSession(int currentTurn) {
    return ClientSession.builder()
        .sessionId("session-id")
        .situationType(SituationType.DATE)
        .status(SessionStatus.REHEARSAL_PLAYING)
        .contextStatus(ContextStatus.COMPLETED)
        .selectedOutfitId("outfit-1")
        .currentTurn(currentTurn)
        .maxTurn(3)
        .build();
  }
}
