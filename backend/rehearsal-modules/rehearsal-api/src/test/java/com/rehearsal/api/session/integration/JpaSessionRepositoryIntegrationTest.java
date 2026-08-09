package com.rehearsal.api.session.integration;

import static com.rehearsal.api.support.SimulationTestFixtures.completedTurn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.OpponentLineStatus;
import com.rehearsal.domain.rehearsal.model.RehearsalResult;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationOutcome;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.repository.SessionRepository;
import com.rehearsal.domain.situation.model.SituationType;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Import(JpaSessionRepositoryIntegrationTest.TransactionTestConfiguration.class)
class JpaSessionRepositoryIntegrationTest {

  @Autowired private SessionRepository sessionRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private FailingSessionTransaction failingSessionTransaction;

  @Test
  @Transactional
  void persistsAndRestoresAllSessionAggregateTables() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    sessionRepository.saveSession(session);
    sessionRepository.saveContext(
        session.getSessionId(),
        SessionContext.from(
            SituationType.DATE,
            Map.of("desired_persona", "warm_natural", "critical_moment", "first_greeting")));
    SimulationTurn turn =
        sessionRepository.saveTurn(completedTurn(session.getSessionId(), 1, "How was your day?"));
    SimulationTurnAttempt attempt =
        sessionRepository.saveAttempt(
            SimulationTurnAttempt.pending(turn.getId(), 1, "It was good."));
    attempt.complete(
        new TurnEvaluationResult(TurnEvaluationOutcome.ACCEPTED, "Natural response", false));
    sessionRepository.saveAttempt(attempt);
    sessionRepository.saveResult(
        RehearsalResult.create(
            session.getSessionId(), "https://video", "ticket", "https://download"));

    entityManager.flush();
    entityManager.clear();

    assertThat(sessionRepository.findSession(session.getSessionId())).isPresent();
    assertThat(sessionRepository.findContext(session.getSessionId()).orElseThrow().values())
        .containsEntry("desired_persona", "warm_natural");
    assertThat(
            sessionRepository
                .findTurn(session.getSessionId(), 1)
                .orElseThrow()
                .getOpponentLineStatus())
        .isEqualTo(OpponentLineStatus.COMPLETED);
    assertThat(sessionRepository.findAttempt(turn.getId(), 1).orElseThrow().getEvaluationStatus())
        .isEqualTo(EvaluationStatus.COMPLETED);
    assertThat(sessionRepository.findResult(session.getSessionId()).orElseThrow().ticketSummary())
        .isEqualTo("ticket");
  }

  @Test
  void outerTransactionRollsBackRepositoryWrites() {
    String sessionId = "rollback-session-id";

    assertThatThrownBy(() -> failingSessionTransaction.saveThenFail(sessionId))
        .isInstanceOf(IllegalStateException.class);

    assertThat(sessionRepository.findSession(sessionId)).isEmpty();
  }

  @TestConfiguration
  static class TransactionTestConfiguration {

    @Bean
    FailingSessionTransaction failingSessionTransaction(SessionRepository sessionRepository) {
      return new FailingSessionTransaction(sessionRepository);
    }
  }

  static class FailingSessionTransaction {

    private final SessionRepository sessionRepository;

    FailingSessionTransaction(SessionRepository sessionRepository) {
      this.sessionRepository = sessionRepository;
    }

    @Transactional
    public void saveThenFail(String sessionId) {
      sessionRepository.saveSession(
          ClientSession.builder()
              .sessionId(sessionId)
              .situationType(SituationType.DATE)
              .status(com.rehearsal.domain.session.model.SessionStatus.BRIEFING)
              .contextStatus(com.rehearsal.domain.session.model.ContextStatus.NOT_STARTED)
              .build());
      throw new IllegalStateException("rollback");
    }
  }
}
