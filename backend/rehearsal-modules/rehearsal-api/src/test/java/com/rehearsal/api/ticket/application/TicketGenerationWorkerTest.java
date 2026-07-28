package com.rehearsal.api.ticket.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.config.ticket.TicketProperties;
import com.rehearsal.api.rehearsal.application.SimulationContextReader;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemorySessionRepository;
import com.rehearsal.api.support.InMemoryTicketJobStore;
import com.rehearsal.api.support.TestClientSessions;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.ticket.model.TicketCopyRawResult;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.model.TicketJobStatus;
import com.rehearsal.domain.ticket.port.TicketCopyGeneratorClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TicketGenerationWorkerTest {

  private static final String FALLBACK_DOWNLOAD_URL =
      "http://localhost:8080/mock-videos/unavailable.webm";

  @Test
  void completesJobAndCompletesSessionOnSuccessfulGeneration() {
    ClientSession session = finishedSession();
    session.assignVideoUrl("http://localhost/mock-videos/test-session-id.webm");
    session.completeVideoUpload();
    InMemorySessionRepository sessionRepository = new InMemorySessionRepository(session);
    InMemoryTicketJobStore jobStore = new InMemoryTicketJobStore();
    TicketGenerationWorker worker =
        workerWith(
            sessionRepository, jobStore, command -> new TicketCopyRawResult("리허설 완료!", "잘 하셨어요."));

    worker.generateAsync(session.getSessionId());

    Optional<TicketJob> job = jobStore.findById(session.getSessionId());
    assertThat(job).isPresent();
    assertThat(job.get().status()).isEqualTo(TicketJobStatus.COMPLETED);
    assertThat(job.get().result().title()).isEqualTo("리허설 완료!");
    assertThat(job.get().result().message()).isEqualTo("잘 하셨어요.");
    assertThat(job.get().result().fallback()).isFalse();
    assertThat(job.get().result().videoAvailable()).isTrue();
    assertThat(job.get().result().downloadUrl())
        .isEqualTo("http://localhost/mock-videos/test-session-id.webm");
    assertThat(job.get().result().qrPayload()).isEqualTo(job.get().result().downloadUrl());
    assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
  }

  @Test
  void completesJobWithCopyFallbackWhenAiCallFails() {
    ClientSession session = finishedSession();
    InMemorySessionRepository sessionRepository = new InMemorySessionRepository(session);
    InMemoryTicketJobStore jobStore = new InMemoryTicketJobStore();
    TicketGenerationWorker worker =
        workerWith(
            sessionRepository,
            jobStore,
            command -> {
              throw new IllegalStateException("Gemini timeout");
            });

    worker.generateAsync(session.getSessionId());

    TicketJob job = jobStore.findById(session.getSessionId()).orElseThrow();
    assertThat(job.status()).isEqualTo(TicketJobStatus.COMPLETED);
    assertThat(job.result().fallback()).isTrue();
    assertThat(job.result().title()).isNotBlank();
    assertThat(job.result().message()).isNotBlank();
    assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
  }

  @Test
  void fallsBackDownloadUrlWhenVideoUploadIsNotCompleted() {
    ClientSession session = finishedSession();
    InMemorySessionRepository sessionRepository = new InMemorySessionRepository(session);
    InMemoryTicketJobStore jobStore = new InMemoryTicketJobStore();
    TicketGenerationWorker worker =
        workerWith(
            sessionRepository, jobStore, command -> new TicketCopyRawResult("리허설 완료!", "잘 하셨어요."));

    worker.generateAsync(session.getSessionId());

    TicketJob job = jobStore.findById(session.getSessionId()).orElseThrow();
    assertThat(job.result().videoAvailable()).isFalse();
    assertThat(job.result().downloadUrl()).isEqualTo(FALLBACK_DOWNLOAD_URL);
    assertThat(job.result().qrPayload()).isEqualTo(FALLBACK_DOWNLOAD_URL);
  }

  @Test
  void marksJobFailedWhenSessionVanishesMidFlight() {
    InMemoryTicketJobStore jobStore = new InMemoryTicketJobStore();
    TicketGenerationWorker worker =
        workerWith(
            new InMemorySessionRepository(),
            jobStore,
            command -> new TicketCopyRawResult("리허설 완료!", "잘 하셨어요."));

    worker.generateAsync("unknown-session-id");

    TicketJob job = jobStore.findById("unknown-session-id").orElseThrow();
    assertThat(job.status()).isEqualTo(TicketJobStatus.FAILED);
    assertThat(job.result()).isNull();
  }

  @Test
  void marksJobFailedWhenSimulationIsNotActuallyCompleted() {
    // AI 카피 생성까지는 성공하지만 completeSimulation()에서 검증에 실패하는 경우를 재현한다.
    ClientSession session = TestClientSessions.sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3);
    InMemorySessionRepository sessionRepository = new InMemorySessionRepository(session);
    InMemoryTicketJobStore jobStore = new InMemoryTicketJobStore();
    TicketGenerationWorker worker =
        workerWith(
            sessionRepository, jobStore, command -> new TicketCopyRawResult("리허설 완료!", "잘 하셨어요."));

    worker.generateAsync(session.getSessionId());

    TicketJob job = jobStore.findById(session.getSessionId()).orElseThrow();
    assertThat(job.status()).isEqualTo(TicketJobStatus.FAILED);
  }

  private TicketGenerationWorker workerWith(
      InMemorySessionRepository sessionRepository,
      InMemoryTicketJobStore jobStore,
      TicketCopyGeneratorClient ticketCopyGeneratorClient) {
    SessionReader sessionReader = new SessionReader(sessionRepository);
    SimulationContextReader simulationContextReader =
        new SimulationContextReader(sessionRepository);
    TicketProperties ticketProperties = new TicketProperties();
    ticketProperties.setDownloadFallbackUrl(FALLBACK_DOWNLOAD_URL);
    return new TicketGenerationWorker(
        sessionReader,
        sessionRepository,
        simulationContextReader,
        ticketCopyGeneratorClient,
        jobStore,
        ticketProperties);
  }

  private ClientSession finishedSession() {
    ClientSession session = TestClientSessions.sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(1);
    session.advanceTurn();
    return session;
  }
}
