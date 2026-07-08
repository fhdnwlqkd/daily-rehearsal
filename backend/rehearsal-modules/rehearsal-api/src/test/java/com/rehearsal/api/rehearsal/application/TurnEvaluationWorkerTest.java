package com.rehearsal.api.rehearsal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemorySessionCache;
import com.rehearsal.api.support.InMemoryTurnEvaluationJobStore;
import com.rehearsal.api.support.TestClientSessions;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJobStatus;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationClient;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TurnEvaluationWorkerTest {

  private static final String FIRST_OPPONENT_LINE = "오는 길 괜찮으셨어요?";

  @Test
  void completesJobAndRecordsTurnOnSuccessfulEvaluation() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    InMemoryTurnEvaluationJobStore jobStore = new InMemoryTurnEvaluationJobStore();
    TurnEvaluationWorker worker =
        workerWith(
            sessionCache, jobStore, command -> new TurnEvaluationRawResult(true, "자연스럽습니다."));

    worker.evaluateAsync(session.getSessionId(), 1, "네, 여유 있게 도착했어요.", null);

    Optional<TurnEvaluationJob> job = jobStore.findById(session.getSessionId(), 1);
    assertThat(job).isPresent();
    assertThat(job.get().status()).isEqualTo(TurnEvaluationJobStatus.COMPLETED);
    assertThat(job.get().result().success()).isTrue();
    assertThat(job.get().result().feedback()).isEqualTo("자연스럽습니다.");
    assertThat(job.get().result().fallback()).isFalse();
    assertThat(session.getCurrentTurn()).isEqualTo(2);
    assertThat(session.getConversationHistory()).hasSize(1);
  }

  @Test
  void completesJobWithFallbackWhenAiCallFails() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    InMemoryTurnEvaluationJobStore jobStore = new InMemoryTurnEvaluationJobStore();
    TurnEvaluationWorker worker =
        workerWith(
            sessionCache,
            jobStore,
            command -> {
              throw new IllegalStateException("Gemini timeout");
            });

    worker.evaluateAsync(session.getSessionId(), 1, "...", null);

    TurnEvaluationJob job = jobStore.findById(session.getSessionId(), 1).orElseThrow();
    assertThat(job.status()).isEqualTo(TurnEvaluationJobStatus.COMPLETED);
    assertThat(job.result().success()).isFalse();
    assertThat(job.result().feedback()).isEqualTo("다시 시도해보세요.");
    assertThat(job.result().fallback()).isTrue();
    assertThat(session.getCurrentTurn()).isEqualTo(1);
  }

  @Test
  void marksJobFailedWhenSessionVanishesMidFlight() {
    InMemoryTurnEvaluationJobStore jobStore = new InMemoryTurnEvaluationJobStore();
    TurnEvaluationWorker worker =
        workerWith(
            new InMemorySessionCache(),
            jobStore,
            command -> new TurnEvaluationRawResult(true, "자연스럽습니다."));

    worker.evaluateAsync("unknown-session-id", 1, "transcript", null);

    TurnEvaluationJob job = jobStore.findById("unknown-session-id", 1).orElseThrow();
    assertThat(job.status()).isEqualTo(TurnEvaluationJobStatus.FAILED);
    assertThat(job.result()).isNull();
  }

  @Test
  void marksJobFailedWhenRecordingTurnResultFails() {
    // REHEARSAL_PLAYING이 아니므로 session은 조회되지만 이후 recordTurn()에서 상태 검증에 실패한다.
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    InMemoryTurnEvaluationJobStore jobStore = new InMemoryTurnEvaluationJobStore();
    TurnEvaluationWorker worker =
        workerWith(
            sessionCache, jobStore, command -> new TurnEvaluationRawResult(true, "자연스럽습니다."));

    worker.evaluateAsync(session.getSessionId(), 1, "transcript", null);

    TurnEvaluationJob job = jobStore.findById(session.getSessionId(), 1).orElseThrow();
    assertThat(job.status()).isEqualTo(TurnEvaluationJobStatus.FAILED);
  }

  private TurnEvaluationWorker workerWith(
      InMemorySessionCache sessionCache,
      InMemoryTurnEvaluationJobStore jobStore,
      TurnEvaluationClient turnEvaluationClient) {
    SessionReader sessionReader = new SessionReader(sessionCache);
    return new TurnEvaluationWorker(sessionReader, sessionCache, turnEvaluationClient, jobStore);
  }

  private ClientSession sessionWith(SessionStatus status) {
    return TestClientSessions.sessionWith(status);
  }
}
