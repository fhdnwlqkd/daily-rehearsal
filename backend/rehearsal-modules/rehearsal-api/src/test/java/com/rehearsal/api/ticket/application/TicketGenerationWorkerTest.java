package com.rehearsal.api.ticket.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.config.decart.DecartProperties;
import com.rehearsal.api.config.ticket.TicketProperties;
import com.rehearsal.api.decart.application.OutfitSpecResolver;
import com.rehearsal.api.rehearsal.application.SimulationContextReader;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemorySessionRepository;
import com.rehearsal.api.support.InMemoryTicketJobStore;
import com.rehearsal.api.support.TestClientSessions;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.ticket.model.ChangeCard;
import com.rehearsal.domain.ticket.model.TicketCopyRawResult;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.model.TicketJobStatus;
import com.rehearsal.domain.ticket.port.TicketCopyGeneratorClient;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TicketGenerationWorkerTest {

  private static final String FALLBACK_DOWNLOAD_URL =
      "http://localhost:8080/mock-videos/unavailable.webm";

  @Test
  void completesJobWithChangeCardSnapshotAndVideo() {
    ClientSession session = finishedSession();
    session.assignVideoUrl("http://localhost/mock-videos/test-session-id.webm");
    session.completeVideoUpload();
    InMemorySessionRepository sessionRepository = new InMemorySessionRepository(session);
    sessionRepository.saveContext(
        session.getSessionId(),
        SessionContext.from(
            session.getSituationType(),
            Map.of("critical_moment", "첫 인사", "desired_persona", "warm_natural")));
    InMemoryTicketJobStore jobStore = new InMemoryTicketJobStore();
    TicketGenerationWorker worker =
        workerWith(sessionRepository, jobStore, command -> new TicketCopyRawResult(changeCard()));

    worker.generateAsync(session.getSessionId());

    Optional<TicketJob> job = jobStore.findById(session.getSessionId());
    assertThat(job).isPresent();
    assertThat(job.get().status()).isEqualTo(TicketJobStatus.COMPLETED);
    assertThat(job.get().result().changeCard()).isEqualTo(changeCard());
    assertThat(job.get().result().snapshot().situationLabel()).isEqualTo("소개팅");
    assertThat(job.get().result().snapshot().criticalMoment()).isEqualTo("첫 인사");
    assertThat(job.get().result().snapshot().desiredPersonaLabel()).isEqualTo("따뜻하고 자연스럽게");
    assertThat(job.get().result().snapshot().selectedOutfitLabel()).isEqualTo("네이비 정장");
    assertThat(job.get().result().fallback()).isFalse();
    assertThat(job.get().result().videoAvailable()).isTrue();
    assertThat(job.get().result().downloadUrl())
        .isEqualTo("http://localhost/mock-videos/test-session-id.webm");
    assertThat(job.get().result().qrPayload()).isEqualTo(job.get().result().downloadUrl());
    assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
  }

  @Test
  void completesJobWithChangeCardFallbackWhenAiCallFails() {
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
    assertThat(job.result().changeCard().todayAction()).isNotBlank();
    assertThat(job.result().changeCard().tomorrowAttitude()).isNotBlank();
    assertThat(job.result().changeCard().ifThenPlan()).isNotBlank();
    assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
  }

  @Test
  void fallsBackDownloadUrlWhenVideoUploadIsNotCompleted() {
    ClientSession session = finishedSession();
    InMemorySessionRepository sessionRepository = new InMemorySessionRepository(session);
    InMemoryTicketJobStore jobStore = new InMemoryTicketJobStore();
    TicketGenerationWorker worker =
        workerWith(sessionRepository, jobStore, command -> new TicketCopyRawResult(changeCard()));

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
            command -> new TicketCopyRawResult(changeCard()));

    worker.generateAsync("unknown-session-id");

    TicketJob job = jobStore.findById("unknown-session-id").orElseThrow();
    assertThat(job.status()).isEqualTo(TicketJobStatus.FAILED);
    assertThat(job.result()).isNull();
  }

  @Test
  void marksJobFailedWhenSimulationIsNotActuallyCompleted() {
    ClientSession session = TestClientSessions.sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3);
    InMemorySessionRepository sessionRepository = new InMemorySessionRepository(session);
    InMemoryTicketJobStore jobStore = new InMemoryTicketJobStore();
    TicketGenerationWorker worker =
        workerWith(sessionRepository, jobStore, command -> new TicketCopyRawResult(changeCard()));

    worker.generateAsync(session.getSessionId());

    TicketJob job = jobStore.findById(session.getSessionId()).orElseThrow();
    assertThat(job.status()).isEqualTo(TicketJobStatus.FAILED);
  }

  private TicketGenerationWorker workerWith(
      InMemorySessionRepository sessionRepository,
      InMemoryTicketJobStore jobStore,
      TicketCopyGeneratorClient ticketCopyGeneratorClient) {
    TicketProperties ticketProperties = new TicketProperties();
    ticketProperties.setDownloadFallbackUrl(FALLBACK_DOWNLOAD_URL);
    return new TicketGenerationWorker(
        new SessionReader(sessionRepository),
        sessionRepository,
        new SimulationContextReader(sessionRepository),
        outfitSpecResolver(),
        ticketCopyGeneratorClient,
        jobStore,
        ticketProperties);
  }

  private OutfitSpecResolver outfitSpecResolver() {
    DecartProperties properties = new DecartProperties();
    DecartProperties.OutfitSpec outfit = new DecartProperties.OutfitSpec();
    outfit.setLabel("네이비 정장");
    properties.getOutfits().put("test-outfit-id", outfit);
    return new OutfitSpecResolver(properties);
  }

  private ClientSession finishedSession() {
    ClientSession session = TestClientSessions.sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(1);
    session.advanceTurn();
    return session;
  }

  private ChangeCard changeCard() {
    return new ChangeCard("첫 문장을 천천히 시작하기", "여유 있게 듣기", "긴장되면 숨을 고르고 말하기");
  }
}
