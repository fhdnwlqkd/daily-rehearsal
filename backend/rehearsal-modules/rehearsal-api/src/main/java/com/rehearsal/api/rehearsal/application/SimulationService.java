package com.rehearsal.api.rehearsal.application;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.OpponentLineJob;
import com.rehearsal.domain.rehearsal.model.OpponentLineJobStatus;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJobStatus;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;
import com.rehearsal.domain.rehearsal.port.OpponentLineJobStore;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationJobStore;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigRegistry;
import com.rehearsal.domain.rehearsal.usecase.GetNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.GetTurnEvaluationUseCase;
import com.rehearsal.domain.rehearsal.usecase.RecordTurnResultUseCase;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitTurnEvaluationUseCase;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Description("고정 N턴 리허설 시뮬레이션 시작, turn 판정/다음 상대 발화 job 제출·조회, turn 결과 누적을 처리하는 application service")
@Service
@RequiredArgsConstructor
public class SimulationService
    implements StartSimulationUseCase,
        RecordTurnResultUseCase,
        SubmitTurnEvaluationUseCase,
        GetTurnEvaluationUseCase,
        SubmitNextOpponentLineUseCase,
        GetNextOpponentLineUseCase {

  private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

  private final SessionCache sessionCache;
  private final SessionReader sessionReader;
  private final TurnEvaluationJobStore turnEvaluationJobStore;
  private final TurnEvaluationWorker turnEvaluationWorker;
  private final OpponentLineJobStore opponentLineJobStore;
  private final NextOpponentLineWorker nextOpponentLineWorker;

  @Override
  public SimulationStart startSimulation(String sessionId) {
    ClientSession session = getValidSession(sessionId);
    RehearsalConfigDefinition config = getConfig(session);

    session.startSimulation(config.maxTurn(), config.firstOpponentLine());
    sessionCache.save(session);

    return new SimulationStart(
        session.getSessionId(),
        session.getCurrentTurn(),
        config.maxTurn(),
        config.firstOpponentLine());
  }

  @Override
  public void recordTurnResult(
      String sessionId, String userTranscript, boolean success, String feedback, boolean fallback) {
    ClientSession session = getValidSession(sessionId);
    session.recordTurn(userTranscript, success, feedback, fallback);
    sessionCache.save(session);
  }

  @Override
  public TurnEvaluationJob submit(
      String sessionId, int turnNo, String userTranscript, TurnMetrics metrics) {
    ClientSession session = getValidSession(sessionId);
    validateTurnNo(session, turnNo);

    Optional<TurnEvaluationJob> existing = turnEvaluationJobStore.findById(sessionId, turnNo);
    if (existing.isPresent() && existing.get().status() != TurnEvaluationJobStatus.FAILED) {
      // 클라이언트의 네트워크 재시도로 인한 중복 제출은 Gemini를 다시 호출하지 않고 기존 job을 그대로 돌려준다.
      return existing.get();
    }

    TurnEvaluationJob pendingJob = TurnEvaluationJob.pending(sessionId, turnNo);
    turnEvaluationJobStore.save(pendingJob);
    try {
      turnEvaluationWorker.evaluateAsync(sessionId, turnNo, userTranscript, metrics);
    } catch (RuntimeException exception) {
      // 스레드풀+큐가 모두 찬 경우 AbortPolicy가 dispatch 시점에 동기적으로 예외를 던진다.
      log.error(
          "Failed to dispatch turn evaluation job for session {} turn {}",
          sessionId,
          turnNo,
          exception);
      TurnEvaluationJob failedJob = pendingJob.fail("작업을 시작하지 못했습니다.");
      turnEvaluationJobStore.save(failedJob);
      return failedJob;
    }
    return pendingJob;
  }

  @Override
  public TurnEvaluationJob get(String sessionId, int turnNo) {
    return turnEvaluationJobStore
        .findById(sessionId, turnNo)
        .orElseThrow(() -> new BusinessException(ErrorCode.TURN_EVALUATION_JOB_NOT_FOUND));
  }

  @Override
  public OpponentLineJob submitNextLine(String sessionId, int turnNo) {
    ClientSession session = getValidSession(sessionId);
    validateTurnNo(session, turnNo);
    validateTurnLimit(session, turnNo);

    Optional<OpponentLineJob> existing = opponentLineJobStore.findById(sessionId, turnNo);
    if (existing.isPresent() && existing.get().status() != OpponentLineJobStatus.FAILED) {
      // 클라이언트의 네트워크 재시도로 인한 중복 제출은 Gemini를 다시 호출하지 않고 기존 job을 그대로 돌려준다.
      return existing.get();
    }

    OpponentLineJob pendingJob = OpponentLineJob.pending(sessionId, turnNo);
    opponentLineJobStore.save(pendingJob);
    try {
      nextOpponentLineWorker.generateAsync(sessionId, turnNo);
    } catch (RuntimeException exception) {
      // 스레드풀+큐가 모두 찬 경우 AbortPolicy가 dispatch 시점에 동기적으로 예외를 던진다.
      log.error(
          "Failed to dispatch next opponent line job for session {} turn {}",
          sessionId,
          turnNo,
          exception);
      OpponentLineJob failedJob = pendingJob.fail("작업을 시작하지 못했습니다.");
      opponentLineJobStore.save(failedJob);
      return failedJob;
    }
    return pendingJob;
  }

  @Override
  public OpponentLineJob getNextLine(String sessionId, int turnNo) {
    return opponentLineJobStore
        .findById(sessionId, turnNo)
        .orElseThrow(() -> new BusinessException(ErrorCode.NEXT_LINE_JOB_NOT_FOUND));
  }

  private void validateTurnNo(ClientSession session, int turnNo) {
    if (session.getCurrentTurn() != turnNo) {
      throw new BusinessException(ErrorCode.SIMULATION_TURN_MISMATCH);
    }
  }

  private void validateTurnLimit(ClientSession session, int turnNo) {
    if (turnNo > session.getMaxTurn()) {
      throw new BusinessException(ErrorCode.SIMULATION_TURN_LIMIT_EXCEEDED);
    }
  }

  private ClientSession getValidSession(String sessionId) {
    return sessionReader.get(sessionId);
  }

  private RehearsalConfigDefinition getConfig(ClientSession session) {
    return RehearsalConfigRegistry.findByType(session.getSituationType())
        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
  }
}
