package com.rehearsal.api.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.rehearsal.api.decart.application.OutfitSpecResolver;
import com.rehearsal.api.support.InMemoryContextExtractionJobStore;
import com.rehearsal.api.support.InMemorySessionRepository;
import com.rehearsal.api.support.RecordingContextExtractionWorker;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.model.ContextExtractionJobStatus;
import com.rehearsal.domain.extraction.model.ContextExtractionJobType;
import com.rehearsal.domain.extraction.port.ContextExtractionJobStore;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.repository.SessionRepository;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SessionServiceTest {

  @Test
  void submitBriefingExtractionCreatesPendingJobAndDispatchesWorker() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    SessionRepository sessionRepository = new InMemorySessionRepository(session);
    ContextExtractionJobStore jobStore = new InMemoryContextExtractionJobStore();
    RecordingContextExtractionWorker worker = new RecordingContextExtractionWorker();
    SessionService service = service(sessionRepository, jobStore, worker);

    ContextExtractionJob job =
        service.submitBriefingExtraction(session.getSessionId(), "briefing transcript");

    assertThat(job.status()).isEqualTo(ContextExtractionJobStatus.PENDING);
    assertThat(job.type()).isEqualTo(ContextExtractionJobType.BRIEFING);
    assertThat(jobStore.findById(session.getSessionId(), job.jobId())).contains(job);
    assertThat(worker.briefingInvocationCount()).isEqualTo(1);
  }

  @Test
  void submitFollowUpExtractionCreatesPendingJobAndDispatchesWorker() {
    ClientSession session = followUpRequiredSession();
    SessionRepository sessionRepository = new InMemorySessionRepository(session);
    ContextExtractionJobStore jobStore = new InMemoryContextExtractionJobStore();
    RecordingContextExtractionWorker worker = new RecordingContextExtractionWorker();
    SessionService service = service(sessionRepository, jobStore, worker);

    ContextExtractionJob job =
        service.submitFollowUpExtraction(session.getSessionId(), "follow-up transcript");

    assertThat(job.status()).isEqualTo(ContextExtractionJobStatus.PENDING);
    assertThat(job.type()).isEqualTo(ContextExtractionJobType.FOLLOW_UP);
    assertThat(jobStore.findById(session.getSessionId(), job.jobId())).contains(job);
    assertThat(worker.followUpInvocationCount()).isEqualTo(1);
  }

  @Test
  void getReturnsContextExtractionJob() {
    ContextExtractionJob job =
        ContextExtractionJob.pending(
            "session-id", SituationType.DATE, ContextExtractionJobType.BRIEFING);
    ContextExtractionJobStore jobStore = new InMemoryContextExtractionJobStore(job);
    SessionService service =
        service(
            new InMemorySessionRepository(ClientSession.create(SituationType.DATE)), jobStore, null);

    ContextExtractionJob found = service.get("session-id", job.jobId());

    assertThat(found).isEqualTo(job);
  }

  private SessionService service(
      SessionRepository sessionRepository,
      ContextExtractionJobStore jobStore,
      RecordingContextExtractionWorker worker) {
    return new SessionService(
        sessionRepository,
        new SessionReader(sessionRepository),
        mock(OutfitSpecResolver.class),
        jobStore,
        worker);
  }

  private ClientSession followUpRequiredSession() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    session.requireFollowUp(
        SessionContext.from(
            SituationType.DATE,
            Map.of("situation_type", "date", "desired_persona", "warm_natural")),
        List.of("critical_moment"),
        List.of("Which moment are you most worried about?"));
    return session;
  }
}
