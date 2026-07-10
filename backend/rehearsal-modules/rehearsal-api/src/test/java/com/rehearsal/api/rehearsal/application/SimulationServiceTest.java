package com.rehearsal.api.rehearsal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemoryOpponentLineJobStore;
import com.rehearsal.api.support.InMemorySessionCache;
import com.rehearsal.api.support.InMemoryTurnEvaluationJobStore;
import com.rehearsal.api.support.RecordingNextOpponentLineWorker;
import com.rehearsal.api.support.RecordingTurnEvaluationWorker;
import com.rehearsal.api.support.TestClientSessions;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.OpponentLineJob;
import com.rehearsal.domain.rehearsal.model.OpponentLineJobStatus;
import com.rehearsal.domain.rehearsal.model.OpponentLineResult;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJobStatus;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import org.junit.jupiter.api.Test;

class SimulationServiceTest {

  private static final String FIRST_OPPONENT_LINE = "오는 길 괜찮으셨어요?";

  @Test
  void startsSimulationForReadySession() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    SimulationService service = serviceWith(session);

    SimulationStart result = service.startSimulation(session.getSessionId());

    assertThat(result.sessionId()).isEqualTo(session.getSessionId());
    assertThat(result.currentTurn()).isEqualTo(1);
    assertThat(result.maxTurn()).isEqualTo(3);
    assertThat(result.opponentLine()).isNotBlank();
    assertThat(session.getStatus()).isEqualTo(SessionStatus.REHEARSAL_PLAYING);
    assertThat(session.getCurrentTurn()).isEqualTo(1);
  }

  @Test
  void startSimulationThrowsSessionNotFound() {
    SimulationService service = serviceWith(new InMemorySessionCache());

    assertThatThrownBy(() -> service.startSimulation("unknown-session-id"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
  }

  @Test
  void startSimulationThrowsInvalidSessionStateWhenNotRehearsalReady() {
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    SimulationService service = serviceWith(session);

    assertThatThrownBy(() -> service.startSimulation(session.getSessionId()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void recordTurnResultPersistsHistoryAndAdvancesTurnOnSuccess() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    SimulationService service = serviceWith(session);

    service.recordTurnResult(session.getSessionId(), "네, 여유 있게 도착했어요.", true, "자연스럽습니다.", false);

    assertThat(session.getCurrentTurn()).isEqualTo(2);
    assertThat(session.getConversationHistory()).hasSize(1);
    assertThat(session.getTurnEvaluations()).hasSize(1);
  }

  @Test
  void recordTurnResultThrowsSessionNotFound() {
    SimulationService service = serviceWith(new InMemorySessionCache());

    assertThatThrownBy(
            () ->
                service.recordTurnResult(
                    "unknown-session-id", "transcript", true, "feedback", false))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
  }

  @Test
  void submitCreatesPendingJobAndDispatchesWorkerWhenNoJobExists() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    RecordingTurnEvaluationWorker worker = new RecordingTurnEvaluationWorker();
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session), new InMemoryTurnEvaluationJobStore(), worker);

    TurnEvaluationJob job = service.submit(session.getSessionId(), 1, "네, 여유 있게 도착했어요.", null);

    assertThat(job.status()).isEqualTo(TurnEvaluationJobStatus.PENDING);
    assertThat(worker.invocationCount()).isEqualTo(1);
  }

  @Test
  void submitReturnsExistingJobWithoutDispatchingWorkerWhenPending() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    TurnEvaluationJob pendingJob = TurnEvaluationJob.pending(session.getSessionId(), 1);
    RecordingTurnEvaluationWorker worker = new RecordingTurnEvaluationWorker();
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session),
            new InMemoryTurnEvaluationJobStore(pendingJob),
            worker);

    TurnEvaluationJob job = service.submit(session.getSessionId(), 1, "transcript", null);

    assertThat(job).isEqualTo(pendingJob);
    assertThat(worker.invocationCount()).isZero();
  }

  @Test
  void submitReturnsExistingJobWithoutDispatchingWorkerWhenCompleted() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    TurnEvaluationJob completedJob =
        TurnEvaluationJob.pending(session.getSessionId(), 1)
            .complete(new TurnEvaluationResult(true, "자연스럽습니다.", false));
    RecordingTurnEvaluationWorker worker = new RecordingTurnEvaluationWorker();
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session),
            new InMemoryTurnEvaluationJobStore(completedJob),
            worker);

    TurnEvaluationJob job = service.submit(session.getSessionId(), 1, "transcript", null);

    assertThat(job).isEqualTo(completedJob);
    assertThat(worker.invocationCount()).isZero();
  }

  @Test
  void submitDispatchesWorkerAgainWhenExistingJobFailed() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    TurnEvaluationJob failedJob =
        TurnEvaluationJob.pending(session.getSessionId(), 1).fail("이전 실행 실패");
    RecordingTurnEvaluationWorker worker = new RecordingTurnEvaluationWorker();
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session),
            new InMemoryTurnEvaluationJobStore(failedJob),
            worker);

    TurnEvaluationJob job = service.submit(session.getSessionId(), 1, "transcript", null);

    assertThat(job.status()).isEqualTo(TurnEvaluationJobStatus.PENDING);
    assertThat(worker.invocationCount()).isEqualTo(1);
  }

  @Test
  void submitThrowsTurnMismatchWhenTurnNoDoesNotMatchCurrentTurn() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    RecordingTurnEvaluationWorker worker = new RecordingTurnEvaluationWorker();
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session), new InMemoryTurnEvaluationJobStore(), worker);

    assertThatThrownBy(() -> service.submit(session.getSessionId(), 2, "transcript", null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SIMULATION_TURN_MISMATCH);
    assertThat(worker.invocationCount()).isZero();
  }

  @Test
  void submitMarksJobFailedWhenWorkerDispatchIsRejected() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    RecordingTurnEvaluationWorker worker = new RecordingTurnEvaluationWorker();
    worker.rejectNextDispatch();
    InMemoryTurnEvaluationJobStore jobStore = new InMemoryTurnEvaluationJobStore();
    SimulationService service = serviceWith(new InMemorySessionCache(session), jobStore, worker);

    TurnEvaluationJob job = service.submit(session.getSessionId(), 1, "transcript", null);

    assertThat(job.status()).isEqualTo(TurnEvaluationJobStatus.FAILED);
    assertThat(jobStore.findById(session.getSessionId(), 1)).contains(job);
  }

  @Test
  void getReturnsStoredJob() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    TurnEvaluationJob completedJob =
        TurnEvaluationJob.pending(session.getSessionId(), 1)
            .complete(new TurnEvaluationResult(true, "자연스럽습니다.", false));
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session),
            new InMemoryTurnEvaluationJobStore(completedJob),
            new RecordingTurnEvaluationWorker());

    TurnEvaluationJob job = service.get(session.getSessionId(), 1);

    assertThat(job).isEqualTo(completedJob);
  }

  @Test
  void getThrowsJobNotFoundWhenNoJobStored() {
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(),
            new InMemoryTurnEvaluationJobStore(),
            new RecordingTurnEvaluationWorker());

    assertThatThrownBy(() -> service.get("unknown-session-id", 1))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.TURN_EVALUATION_JOB_NOT_FOUND);
  }

  @Test
  void submitNextLineCreatesPendingJobAndDispatchesWorkerWhenNoJobExists() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    RecordingNextOpponentLineWorker worker = new RecordingNextOpponentLineWorker();
    SimulationService service =
        serviceWith(new InMemorySessionCache(session), new InMemoryOpponentLineJobStore(), worker);

    OpponentLineJob job = service.submitNextLine(session.getSessionId(), 1);

    assertThat(job.status()).isEqualTo(OpponentLineJobStatus.PENDING);
    assertThat(worker.invocationCount()).isEqualTo(1);
  }

  @Test
  void submitNextLineReturnsExistingJobWithoutDispatchingWorkerWhenPending() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    OpponentLineJob pendingJob = OpponentLineJob.pending(session.getSessionId(), 1);
    RecordingNextOpponentLineWorker worker = new RecordingNextOpponentLineWorker();
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session),
            new InMemoryOpponentLineJobStore(pendingJob),
            worker);

    OpponentLineJob job = service.submitNextLine(session.getSessionId(), 1);

    assertThat(job).isEqualTo(pendingJob);
    assertThat(worker.invocationCount()).isZero();
  }

  @Test
  void submitNextLineDispatchesWorkerAgainWhenExistingJobFailed() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    OpponentLineJob failedJob = OpponentLineJob.pending(session.getSessionId(), 1).fail("이전 실행 실패");
    RecordingNextOpponentLineWorker worker = new RecordingNextOpponentLineWorker();
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session), new InMemoryOpponentLineJobStore(failedJob), worker);

    OpponentLineJob job = service.submitNextLine(session.getSessionId(), 1);

    assertThat(job.status()).isEqualTo(OpponentLineJobStatus.PENDING);
    assertThat(worker.invocationCount()).isEqualTo(1);
  }

  @Test
  void submitNextLineThrowsTurnMismatchWhenTurnNoDoesNotMatchCurrentTurn() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    RecordingNextOpponentLineWorker worker = new RecordingNextOpponentLineWorker();
    SimulationService service =
        serviceWith(new InMemorySessionCache(session), new InMemoryOpponentLineJobStore(), worker);

    assertThatThrownBy(() -> service.submitNextLine(session.getSessionId(), 2))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SIMULATION_TURN_MISMATCH);
    assertThat(worker.invocationCount()).isZero();
  }

  @Test
  void submitNextLineThrowsTurnLimitExceededWhenPastMaxTurn() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(1, FIRST_OPPONENT_LINE);
    session.recordTurn("네, 여유 있게 도착했어요.", true, "자연스럽습니다.", false);
    RecordingNextOpponentLineWorker worker = new RecordingNextOpponentLineWorker();
    SimulationService service =
        serviceWith(new InMemorySessionCache(session), new InMemoryOpponentLineJobStore(), worker);

    assertThatThrownBy(() -> service.submitNextLine(session.getSessionId(), 2))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SIMULATION_TURN_LIMIT_EXCEEDED);
    assertThat(worker.invocationCount()).isZero();
  }

  @Test
  void submitNextLineMarksJobFailedWhenWorkerDispatchIsRejected() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    session.startSimulation(3, FIRST_OPPONENT_LINE);
    RecordingNextOpponentLineWorker worker = new RecordingNextOpponentLineWorker();
    worker.rejectNextDispatch();
    InMemoryOpponentLineJobStore jobStore = new InMemoryOpponentLineJobStore();
    SimulationService service = serviceWith(new InMemorySessionCache(session), jobStore, worker);

    OpponentLineJob job = service.submitNextLine(session.getSessionId(), 1);

    assertThat(job.status()).isEqualTo(OpponentLineJobStatus.FAILED);
    assertThat(jobStore.findById(session.getSessionId(), 1)).contains(job);
  }

  @Test
  void getNextLineReturnsStoredJob() {
    ClientSession session = sessionWith(SessionStatus.REHEARSAL_READY);
    OpponentLineJob completedJob =
        OpponentLineJob.pending(session.getSessionId(), 1)
            .complete(new OpponentLineResult("다음 발화입니다.", false));
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(session),
            new InMemoryOpponentLineJobStore(completedJob),
            new RecordingNextOpponentLineWorker());

    OpponentLineJob job = service.getNextLine(session.getSessionId(), 1);

    assertThat(job).isEqualTo(completedJob);
  }

  @Test
  void getNextLineThrowsJobNotFoundWhenNoJobStored() {
    SimulationService service =
        serviceWith(
            new InMemorySessionCache(),
            new InMemoryOpponentLineJobStore(),
            new RecordingNextOpponentLineWorker());

    assertThatThrownBy(() -> service.getNextLine("unknown-session-id", 1))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.NEXT_LINE_JOB_NOT_FOUND);
  }

  private SimulationService serviceWith(ClientSession session) {
    return serviceWith(
        new InMemorySessionCache(session),
        new InMemoryTurnEvaluationJobStore(),
        new RecordingTurnEvaluationWorker());
  }

  private SimulationService serviceWith(InMemorySessionCache sessionCache) {
    return serviceWith(
        sessionCache, new InMemoryTurnEvaluationJobStore(), new RecordingTurnEvaluationWorker());
  }

  private SimulationService serviceWith(
      InMemorySessionCache sessionCache,
      InMemoryTurnEvaluationJobStore jobStore,
      RecordingTurnEvaluationWorker worker) {
    return new SimulationService(
        sessionCache,
        new SessionReader(sessionCache),
        jobStore,
        worker,
        new InMemoryOpponentLineJobStore(),
        new RecordingNextOpponentLineWorker());
  }

  private SimulationService serviceWith(
      InMemorySessionCache sessionCache,
      InMemoryOpponentLineJobStore opponentLineJobStore,
      RecordingNextOpponentLineWorker nextOpponentLineWorker) {
    return new SimulationService(
        sessionCache,
        new SessionReader(sessionCache),
        new InMemoryTurnEvaluationJobStore(),
        new RecordingTurnEvaluationWorker(),
        opponentLineJobStore,
        nextOpponentLineWorker);
  }

  private ClientSession sessionWith(SessionStatus status) {
    return TestClientSessions.sessionWith(status);
  }
}
