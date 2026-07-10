package com.rehearsal.api.rehearsal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemoryOpponentLineJobStore;
import com.rehearsal.api.support.InMemorySessionCache;
import com.rehearsal.api.support.TestClientSessions;
import com.rehearsal.domain.rehearsal.model.OpponentLineJob;
import com.rehearsal.domain.rehearsal.model.OpponentLineJobStatus;
import com.rehearsal.domain.rehearsal.port.OpponentLineGeneratorClient;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NextOpponentLineWorkerTest {

  private static final String FIRST_OPPONENT_LINE = "오는 길 괜찮으셨어요?";

  @Test
  void completesJobAndUpdatesCurrentOpponentLineOnSuccess() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    InMemoryOpponentLineJobStore jobStore = new InMemoryOpponentLineJobStore();
    NextOpponentLineWorker worker = workerWith(sessionCache, jobStore, command -> "다음 발화입니다.");

    worker.generateAsync(session.getSessionId(), 1);

    Optional<OpponentLineJob> job = jobStore.findById(session.getSessionId(), 1);
    assertThat(job).isPresent();
    assertThat(job.get().status()).isEqualTo(OpponentLineJobStatus.COMPLETED);
    assertThat(job.get().result().opponentLine()).isEqualTo("다음 발화입니다.");
    assertThat(job.get().result().fallback()).isFalse();
    assertThat(session.getCurrentOpponentLine()).isEqualTo("다음 발화입니다.");
  }

  @Test
  void completesJobWithFallbackWhenAiCallFails() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    InMemoryOpponentLineJobStore jobStore = new InMemoryOpponentLineJobStore();
    NextOpponentLineWorker worker =
        workerWith(
            sessionCache,
            jobStore,
            command -> {
              throw new IllegalStateException("Gemini timeout");
            });

    worker.generateAsync(session.getSessionId(), 1);

    OpponentLineJob job = jobStore.findById(session.getSessionId(), 1).orElseThrow();
    assertThat(job.status()).isEqualTo(OpponentLineJobStatus.COMPLETED);
    assertThat(job.result().fallback()).isTrue();
    assertThat(job.result().opponentLine()).isNotBlank();
    assertThat(session.getCurrentOpponentLine()).isEqualTo(job.result().opponentLine());
  }

  @Test
  void marksJobFailedWhenSessionVanishesMidFlight() {
    InMemoryOpponentLineJobStore jobStore = new InMemoryOpponentLineJobStore();
    NextOpponentLineWorker worker =
        workerWith(new InMemorySessionCache(), jobStore, command -> "다음 발화입니다.");

    worker.generateAsync("unknown-session-id", 1);

    OpponentLineJob job = jobStore.findById("unknown-session-id", 1).orElseThrow();
    assertThat(job.status()).isEqualTo(OpponentLineJobStatus.FAILED);
    assertThat(job.result()).isNull();
  }

  @Test
  void marksJobFailedWhenUpdatingOpponentLineFails() {
    // REHEARSAL_PLAYING이 아니므로 session은 조회되지만 이후 updateOpponentLine()에서 상태 검증에 실패한다.
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    InMemoryOpponentLineJobStore jobStore = new InMemoryOpponentLineJobStore();
    NextOpponentLineWorker worker = workerWith(sessionCache, jobStore, command -> "다음 발화입니다.");

    worker.generateAsync(session.getSessionId(), 1);

    OpponentLineJob job = jobStore.findById(session.getSessionId(), 1).orElseThrow();
    assertThat(job.status()).isEqualTo(OpponentLineJobStatus.FAILED);
  }

  private NextOpponentLineWorker workerWith(
      InMemorySessionCache sessionCache,
      InMemoryOpponentLineJobStore jobStore,
      OpponentLineGeneratorClient opponentLineGeneratorClient) {
    SessionReader sessionReader = new SessionReader(sessionCache);
    return new NextOpponentLineWorker(
        sessionReader, sessionCache, opponentLineGeneratorClient, jobStore);
  }

  private ClientSession sessionWith(SessionStatus status) {
    return TestClientSessions.sessionWith(status);
  }
}
