package com.rehearsal.api.rehearsal.application;

import com.rehearsal.api.config.async.AsyncConfig;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationCommand;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationOutcome;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationClient;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigRegistry;
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
  private static final String AI_FAILURE_FEEDBACK = "답변을 확인했습니다. 다음 단계로 진행할게요.";
  private static final String ATTEMPTS_EXHAUSTED_FEEDBACK = "두 번의 연습을 마쳤어요. 다음 단계로 넘어갈게요.";

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
      if (result.outcome() == TurnEvaluationOutcome.FORCED_ADVANCE && !result.fallback()) {
        result =
            new TurnEvaluationResult(
                TurnEvaluationOutcome.FORCED_ADVANCE,
                feedbackWithProgress(result.feedback()),
                false);
      }

      attempt.complete(result);
      sessionRepository.saveAttempt(attempt);
      if (result.outcome().advancesTurn()) {
        session.advanceTurn();
        sessionRepository.saveSession(session);
      }
    } catch (RuntimeException exception) {
      log.error(
          "Turn evaluation worker failed for session {} turn {}",
          event.sessionId(),
          event.turnNo(),
          exception);
      recoverAttempt(event, exception);
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
            turn.getPlan().sceneCue(),
            turn.getPlan().opponentLine(),
            turn.getPlan().actionPrompt(),
            turn.getPlan().acceptedIntentHint(),
            RehearsalConfigRegistry.findByType(session.getSituationType())
                .orElseThrow()
                .feedbackFocus(),
            attempt.getUserTranscript(),
            event.metrics());
    try {
      TurnEvaluationRawResult raw = turnEvaluationClient.evaluate(command);
      return TurnEvaluationResult.classify(
          raw.accepted(),
          attempt.getAttemptNo(),
          requiredConfig(session).maxAttemptsPerTurn(),
          raw.feedback(),
          false);
    } catch (RuntimeException exception) {
      log.warn("Turn evaluation AI call failed for session {}", session.getSessionId(), exception);
      return new TurnEvaluationResult(
          TurnEvaluationOutcome.FORCED_ADVANCE, AI_FAILURE_FEEDBACK, true);
    }
  }

  private void recoverAttempt(TurnEvaluationRequested event, RuntimeException exception) {
    try {
      SimulationTurn turn = requiredTurn(event.sessionId(), event.turnNo());
      SimulationTurnAttempt attempt = requiredAttempt(turn.getId(), event.attemptNo());
      if (attempt.getEvaluationStatus() == EvaluationStatus.PENDING) {
        if (attempt.canRetry()) {
          attempt.fail(exception.getMessage());
          sessionRepository.saveAttempt(attempt);
          return;
        }
        attempt.complete(
            new TurnEvaluationResult(
                TurnEvaluationOutcome.FORCED_ADVANCE, ATTEMPTS_EXHAUSTED_FEEDBACK, true));
        sessionRepository.saveAttempt(attempt);
      }
      if (attempt.getEvaluationStatus() == EvaluationStatus.COMPLETED
          && attempt.getOutcome() != null
          && attempt.getOutcome().advancesTurn()) {
        advanceSessionIfStillOnTurn(event);
      }
    } catch (RuntimeException recoveryException) {
      log.error(
          "Turn evaluation failure recovery failed for session {} turn {}",
          event.sessionId(),
          event.turnNo(),
          recoveryException);
    }
  }

  private void advanceSessionIfStillOnTurn(TurnEvaluationRequested event) {
    ClientSession session = sessionReader.get(event.sessionId());
    if (session.getCurrentTurn() != event.turnNo()) {
      return;
    }
    session.advanceTurn();
    sessionRepository.saveSession(session);
  }

  private SimulationTurn requiredTurn(String sessionId, int turnNo) {
    return sessionRepository.findTurn(sessionId, turnNo).orElseThrow();
  }

  private SimulationTurnAttempt requiredAttempt(Long turnId, int attemptNo) {
    return sessionRepository.findAttempt(turnId, attemptNo).orElseThrow();
  }

  private RehearsalConfigDefinition requiredConfig(ClientSession session) {
    return RehearsalConfigRegistry.findByType(session.getSituationType()).orElseThrow();
  }

  private String feedbackWithProgress(String feedback) {
    if (feedback == null || feedback.isBlank()) {
      return ATTEMPTS_EXHAUSTED_FEEDBACK;
    }
    return "%s %s".formatted(feedback.strip(), ATTEMPTS_EXHAUSTED_FEEDBACK);
  }
}
