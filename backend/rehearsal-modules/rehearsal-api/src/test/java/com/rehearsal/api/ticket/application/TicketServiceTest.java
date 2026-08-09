package com.rehearsal.api.ticket.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemorySessionRepository;
import com.rehearsal.api.support.InMemoryTicketJobStore;
import com.rehearsal.api.support.RecordingTicketGenerationWorker;
import com.rehearsal.api.support.TestClientSessions;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.model.TicketJobStatus;
import com.rehearsal.domain.ticket.model.TicketPayload;
import org.junit.jupiter.api.Test;

class TicketServiceTest {

  @Test
  void submitCreatesPendingJobAndDispatchesWorkerWhenSimulationFinished() {
    ClientSession session = finishedSession();
    RecordingTicketGenerationWorker worker = new RecordingTicketGenerationWorker();
    TicketService service =
        serviceWith(new InMemorySessionRepository(session), new InMemoryTicketJobStore(), worker);

    TicketJob job = service.submit(session.getSessionId());

    assertThat(job.status()).isEqualTo(TicketJobStatus.PENDING);
    assertThat(worker.invocationCount()).isEqualTo(1);
  }

  @Test
  void submitThrowsSimulationNotCompletedWhenTurnsRemain() {
    ClientSession session = TestClientSessions.sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3);
    RecordingTicketGenerationWorker worker = new RecordingTicketGenerationWorker();
    TicketService service =
        serviceWith(new InMemorySessionRepository(session), new InMemoryTicketJobStore(), worker);

    assertThatThrownBy(() -> service.submit(session.getSessionId()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SIMULATION_NOT_COMPLETED);
    assertThat(worker.invocationCount()).isZero();
  }

  @Test
  void submitThrowsSimulationNotCompletedWhenNotPlaying() {
    ClientSession session = TestClientSessions.sessionWith(SessionStatus.TRANSFORMATION_READY);
    RecordingTicketGenerationWorker worker = new RecordingTicketGenerationWorker();
    TicketService service =
        serviceWith(new InMemorySessionRepository(session), new InMemoryTicketJobStore(), worker);

    assertThatThrownBy(() -> service.submit(session.getSessionId()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SIMULATION_NOT_COMPLETED);
    assertThat(worker.invocationCount()).isZero();
  }

  @Test
  void submitThrowsSessionNotFound() {
    TicketService service =
        serviceWith(
            new InMemorySessionRepository(),
            new InMemoryTicketJobStore(),
            new RecordingTicketGenerationWorker());

    assertThatThrownBy(() -> service.submit("unknown-session-id"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
  }

  @Test
  void submitReturnsExistingJobWithoutDispatchingWorkerWhenPending() {
    ClientSession session = finishedSession();
    TicketJob pendingJob = TicketJob.pending(session.getSessionId());
    RecordingTicketGenerationWorker worker = new RecordingTicketGenerationWorker();
    TicketService service =
        serviceWith(
            new InMemorySessionRepository(session), new InMemoryTicketJobStore(pendingJob), worker);

    TicketJob job = service.submit(session.getSessionId());

    assertThat(job).isEqualTo(pendingJob);
    assertThat(worker.invocationCount()).isZero();
  }

  @Test
  void submitReturnsExistingJobWithoutDispatchingWorkerWhenCompleted() {
    ClientSession session = finishedSession();
    TicketJob completedJob = TicketJob.pending(session.getSessionId()).complete(samplePayload());
    RecordingTicketGenerationWorker worker = new RecordingTicketGenerationWorker();
    TicketService service =
        serviceWith(
            new InMemorySessionRepository(session),
            new InMemoryTicketJobStore(completedJob),
            worker);

    TicketJob job = service.submit(session.getSessionId());

    assertThat(job).isEqualTo(completedJob);
    assertThat(worker.invocationCount()).isZero();
  }

  @Test
  void submitDispatchesWorkerAgainWhenExistingJobFailed() {
    ClientSession session = finishedSession();
    TicketJob failedJob = TicketJob.pending(session.getSessionId()).fail("이전 실행 실패");
    RecordingTicketGenerationWorker worker = new RecordingTicketGenerationWorker();
    TicketService service =
        serviceWith(
            new InMemorySessionRepository(session), new InMemoryTicketJobStore(failedJob), worker);

    TicketJob job = service.submit(session.getSessionId());

    assertThat(job.status()).isEqualTo(TicketJobStatus.PENDING);
    assertThat(worker.invocationCount()).isEqualTo(1);
  }

  @Test
  void submitMarksJobFailedWhenWorkerDispatchIsRejected() {
    ClientSession session = finishedSession();
    RecordingTicketGenerationWorker worker = new RecordingTicketGenerationWorker();
    worker.rejectNextDispatch();
    InMemoryTicketJobStore jobStore = new InMemoryTicketJobStore();
    TicketService service = serviceWith(new InMemorySessionRepository(session), jobStore, worker);

    TicketJob job = service.submit(session.getSessionId());

    assertThat(job.status()).isEqualTo(TicketJobStatus.FAILED);
    assertThat(jobStore.findById(session.getSessionId())).contains(job);
  }

  @Test
  void getReturnsStoredJob() {
    ClientSession session = finishedSession();
    TicketJob completedJob = TicketJob.pending(session.getSessionId()).complete(samplePayload());
    TicketService service =
        serviceWith(
            new InMemorySessionRepository(session),
            new InMemoryTicketJobStore(completedJob),
            new RecordingTicketGenerationWorker());

    TicketJob job = service.get(session.getSessionId());

    assertThat(job).isEqualTo(completedJob);
  }

  @Test
  void getThrowsTicketJobNotFoundWhenNoJobStored() {
    TicketService service =
        serviceWith(
            new InMemorySessionRepository(),
            new InMemoryTicketJobStore(),
            new RecordingTicketGenerationWorker());

    assertThatThrownBy(() -> service.get("unknown-session-id"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.TICKET_JOB_NOT_FOUND);
  }

  private ClientSession finishedSession() {
    ClientSession session = TestClientSessions.sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(1);
    session.advanceTurn();
    return session;
  }

  private TicketPayload samplePayload() {
    return new TicketPayload(
        new com.rehearsal.domain.ticket.model.TicketSnapshot("소개팅", "첫 인사", "따뜻하고 자연스럽게", "네이비 정장"),
        new com.rehearsal.domain.ticket.model.ChangeCard(
            "첫 문장을 천천히 시작하기", "여유 있게 듣기", "긴장되면 숨을 고르고 말하기"),
        false,
        "http://localhost/mock-videos/test-session-id.webm",
        true,
        "http://localhost/mock-videos/test-session-id.webm",
        "http://localhost/mock-videos/test-session-id.webm");
  }

  private TicketService serviceWith(
      InMemorySessionRepository sessionRepository,
      InMemoryTicketJobStore jobStore,
      RecordingTicketGenerationWorker worker) {
    return new TicketService(new SessionReader(sessionRepository), jobStore, worker);
  }
}
