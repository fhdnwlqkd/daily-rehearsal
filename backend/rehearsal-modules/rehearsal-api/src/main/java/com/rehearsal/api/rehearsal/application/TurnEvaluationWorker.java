package com.rehearsal.api.rehearsal.application;

import com.rehearsal.api.config.async.AsyncConfig;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationCommand;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationClient;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Description("Worker that stores turn evaluation results in simulation_turn_attempt")
@Component
@RequiredArgsConstructor
public class TurnEvaluationWorker {

  private static final Logger log = LoggerFactory.getLogger(TurnEvaluationWorker.class);
  private static final String AI_FAILURE_FEEDBACK = "Please try again.";

  private final SessionReader sessionReader;
  private final SessionRepository sessionRepository;
  private final SimulationContextReader simulationContextReader;
  private final TurnEvaluationClient turnEvaluationClient;

  @Async(AsyncConfig.TURN_EVALUATION_EXECUTOR)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void evaluateAsync(TurnEvaluationRequested event) {
    evaluate(event);
  }

  public void evaluate(TurnEvaluationRequested event) {
    try {
      ClientSession session = sessionReader.get(event.sessionId());
      SimulationTurn turn = requiredTurn(event.sessionId(), event.turnNo());
      SimulationTurnAttempt attempt = requiredAttempt(turn.getId(), event.attemptNo());
      TurnEvaluationResult result = evaluate(session, turn, attempt, event);

      attempt.complete(result);
      sessionRepository.saveAttempt(attempt);
      if (result.success()) {
        session.advanceTurn();
        sessionRepository.saveSession(session);
      }
    } catch (RuntimeException exception) {
      log.error(
          "Turn evaluation worker failed for session {} turn {}",
          event.sessionId(),
          event.turnNo(),
          exception);
      failAttempt(event, exception);
    }
  }

  private TurnEvaluationResult evaluate(
      ClientSession session,
      SimulationTurn turn,
      SimulationTurnAttempt attempt,
      TurnEvaluationRequested event) {
    TurnEvaluationCommand command =
        new TurnEvaluationCommand(
            session.getSituationType(),
            simulationContextReader.context(session),
            session.getSelectedOutfitId(),
            simulationContextReader.history(session.getSessionId(), event.turnNo()),
            event.turnNo(),
            turn.getOpponentLine(),
            attempt.getUserTranscript(),
            event.metrics());
    try {
      TurnEvaluationRawResult raw = turnEvaluationClient.evaluate(command);
      return new TurnEvaluationResult(raw.success(), raw.feedback(), false);
    } catch (RuntimeException exception) {
      log.warn("Turn evaluation AI call failed for session {}", session.getSessionId(), exception);
      return new TurnEvaluationResult(false, AI_FAILURE_FEEDBACK, true);
    }
  }

  private void failAttempt(TurnEvaluationRequested event, RuntimeException exception) {
    sessionRepository
        .findTurn(event.sessionId(), event.turnNo())
        .flatMap(turn -> sessionRepository.findAttempt(turn.getId(), event.attemptNo()))
        .filter(attempt -> attempt.getEvaluationStatus() == EvaluationStatus.PENDING)
        .ifPresent(
            attempt -> {
              attempt.fail(exception.getMessage());
              sessionRepository.saveAttempt(attempt);
            });
  }

  private SimulationTurn requiredTurn(String sessionId, int turnNo) {
    return sessionRepository.findTurn(sessionId, turnNo).orElseThrow();
  }

  private SimulationTurnAttempt requiredAttempt(Long turnId, int attemptNo) {
    return sessionRepository.findAttempt(turnId, attemptNo).orElseThrow();
  }
}
