package com.rehearsal.api.rehearsal.application;

import com.rehearsal.api.config.async.AsyncConfig;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import com.rehearsal.domain.rehearsal.model.OpponentLineResult;
import com.rehearsal.domain.rehearsal.model.OpponentLineStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.rehearsal.port.OpponentLineGeneratorClient;
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

@Description("Worker that stores generated simulation turn plans")
@Component
@RequiredArgsConstructor
public class NextOpponentLineWorker {

  private static final Logger log = LoggerFactory.getLogger(NextOpponentLineWorker.class);

  private final SessionReader sessionReader;
  private final SessionRepository sessionRepository;
  private final SimulationContextReader simulationContextReader;
  private final OpponentLineGeneratorClient opponentLineGeneratorClient;

  @Async(AsyncConfig.NEXT_LINE_EXECUTOR)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void generateAsync(OpponentLineRequested event) {
    generate(event);
  }

  public void generate(OpponentLineRequested event) {
    try {
      ClientSession session = sessionReader.get(event.sessionId());
      SimulationTurn turn = requiredTurn(event.sessionId(), event.turnNo());
      OpponentLineResult result = generate(session, turn);
      if (result.fallback()) {
        turn.completeWithTechnicalFallback(result.plan());
      } else {
        turn.complete(result.plan());
      }
      sessionRepository.saveTurn(turn);
    } catch (RuntimeException exception) {
      log.error(
          "Opponent line worker failed for session {} turn {}",
          event.sessionId(),
          event.turnNo(),
          exception);
      failTurn(event, exception);
    }
  }

  private OpponentLineResult generate(ClientSession session, SimulationTurn turn) {
    RehearsalConfigDefinition config = config(session);
    OpponentLineCommand command =
        new OpponentLineCommand(
            session.getSituationType(),
            simulationContextReader.context(session),
            session.getSelectedOutfitId(),
            simulationContextReader.history(session.getSessionId(), turn.getTurnNo()),
            turn.getTurnNo(),
            turn.getGenerationMode(),
            config.objectiveFor(turn.getTurnNo()),
            config.feedbackFocus(),
            config.recoveryDirection(),
            previousTurnPlan(session.getSessionId(), turn.getTurnNo()));
    try {
      return new OpponentLineResult(opponentLineGeneratorClient.generate(command), false);
    } catch (RuntimeException exception) {
      log.warn("Opponent line AI call failed for session {}", session.getSessionId(), exception);
      return new OpponentLineResult(config.technicalFallback(), true);
    }
  }

  private SimulationTurnPlan previousTurnPlan(String sessionId, int turnNo) {
    if (turnNo <= 1) {
      return null;
    }
    return sessionRepository
        .findTurn(sessionId, turnNo - 1)
        .map(SimulationTurn::getPlan)
        .orElse(null);
  }

  private void failTurn(OpponentLineRequested event, RuntimeException exception) {
    sessionRepository
        .findTurn(event.sessionId(), event.turnNo())
        .filter(turn -> turn.getOpponentLineStatus() == OpponentLineStatus.PENDING)
        .ifPresent(
            turn -> {
              turn.fail(exception.getMessage());
              sessionRepository.saveTurn(turn);
            });
  }

  private SimulationTurn requiredTurn(String sessionId, int turnNo) {
    return sessionRepository.findTurn(sessionId, turnNo).orElseThrow();
  }

  private RehearsalConfigDefinition config(ClientSession session) {
    return RehearsalConfigRegistry.findByType(session.getSituationType())
        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
  }
}
