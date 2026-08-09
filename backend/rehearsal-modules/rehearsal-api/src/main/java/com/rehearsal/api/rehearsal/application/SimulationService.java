package com.rehearsal.api.rehearsal.application;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.OpponentLineStatus;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigRegistry;
import com.rehearsal.domain.rehearsal.usecase.GetNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.GetTurnEvaluationUseCase;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitTurnEvaluationUseCase;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Description("Application service for RDB-backed simulation turns and attempts")
@Service
@RequiredArgsConstructor
public class SimulationService
    implements StartSimulationUseCase,
        SubmitTurnEvaluationUseCase,
        GetTurnEvaluationUseCase,
        SubmitNextOpponentLineUseCase,
        GetNextOpponentLineUseCase {

  private final SessionRepository sessionRepository;
  private final SessionReader sessionReader;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public SimulationStart startSimulation(String sessionId) {
    ClientSession session = sessionReader.getForUpdate(sessionId);
    RehearsalConfigDefinition config = config(session);

    if (session.getStatus() == SessionStatus.REHEARSAL_PLAYING) {
      SimulationTurn firstTurn = requiredTurn(sessionId, 1);
      return new SimulationStart(sessionId, 1, session.getMaxTurn(), firstTurn.getOpponentLine());
    }

    session.startSimulation(config.maxTurn());
    sessionRepository.saveSession(session);
    sessionRepository.saveTurn(
        SimulationTurn.completed(sessionId, session.getCurrentTurn(), config.firstOpponentLine()));

    return new SimulationStart(
        sessionId, session.getCurrentTurn(), config.maxTurn(), config.firstOpponentLine());
  }

  @Override
  @Transactional
  public SimulationTurnAttempt submit(
      String sessionId, int turnNo, String userTranscript, TurnMetrics metrics) {
    ClientSession session = sessionReader.get(sessionId);
    validateTurnNo(session, turnNo);
    SimulationTurn turn = requiredTurn(sessionId, turnNo);
    if (turn.getOpponentLineStatus() != OpponentLineStatus.COMPLETED) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }

    SimulationTurnAttempt latest =
        sessionRepository.findLatestAttempt(sessionId, turnNo).orElse(null);
    if (latest != null
        && (latest.getEvaluationStatus() == EvaluationStatus.PENDING
            || Boolean.TRUE.equals(latest.getSuccess()))) {
      return latest;
    }

    int attemptNo = latest == null ? 1 : latest.getAttemptNo() + 1;
    SimulationTurnAttempt pending =
        sessionRepository.saveAttempt(
            SimulationTurnAttempt.pending(turn.getId(), attemptNo, userTranscript));
    eventPublisher.publishEvent(new TurnEvaluationRequested(sessionId, turnNo, attemptNo, metrics));
    return pending;
  }

  @Override
  @Transactional(readOnly = true)
  public SimulationTurnAttempt get(String sessionId, int turnNo) {
    sessionReader.get(sessionId);
    return sessionRepository
        .findLatestAttempt(sessionId, turnNo)
        .orElseThrow(() -> new BusinessException(ErrorCode.TURN_EVALUATION_JOB_NOT_FOUND));
  }

  @Override
  @Transactional
  public SimulationTurn submitNextLine(String sessionId, int turnNo) {
    ClientSession session = sessionReader.get(sessionId);
    validateTurnNo(session, turnNo);
    validateTurnLimit(session, turnNo);

    SimulationTurn existing = sessionRepository.findTurn(sessionId, turnNo).orElse(null);
    if (existing != null && existing.getOpponentLineStatus() != OpponentLineStatus.FAILED) {
      return existing;
    }

    SimulationTurn pending = sessionRepository.saveTurn(SimulationTurn.pending(sessionId, turnNo));
    eventPublisher.publishEvent(new OpponentLineRequested(sessionId, turnNo));
    return pending;
  }

  @Override
  @Transactional(readOnly = true)
  public SimulationTurn getNextLine(String sessionId, int turnNo) {
    sessionReader.get(sessionId);
    return requiredTurn(sessionId, turnNo);
  }

  private SimulationTurn requiredTurn(String sessionId, int turnNo) {
    return sessionRepository
        .findTurn(sessionId, turnNo)
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

  private RehearsalConfigDefinition config(ClientSession session) {
    return RehearsalConfigRegistry.findByType(session.getSituationType())
        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
  }
}
