package com.rehearsal.api.rehearsal.application;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationCommand;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationClient;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigRegistry;
import com.rehearsal.domain.rehearsal.usecase.EvaluateTurnUseCase;
import com.rehearsal.domain.rehearsal.usecase.RecordTurnResultUseCase;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Description("고정 N턴 리허설 시뮬레이션 시작, turn 판정, turn 결과 누적을 처리하는 application service")
@Service
@RequiredArgsConstructor
public class SimulationService
    implements StartSimulationUseCase, RecordTurnResultUseCase, EvaluateTurnUseCase {

  private static final Logger log = LoggerFactory.getLogger(SimulationService.class);
  private static final String AI_FAILURE_FEEDBACK = "다시 시도해보세요.";

  private final SessionCache sessionCache;
  private final SessionReader sessionReader;
  private final TurnEvaluationClient turnEvaluationClient;

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
  public TurnEvaluationResult evaluateTurn(
      String sessionId, int turnNo, String userTranscript, TurnMetrics metrics) {
    ClientSession session = getValidSession(sessionId);
    validateTurnNo(session, turnNo);

    TurnEvaluationResult result = evaluate(session, userTranscript, metrics);

    session.recordTurn(userTranscript, result.success(), result.feedback(), result.fallback());
    sessionCache.save(session);

    return result;
  }

  private TurnEvaluationResult evaluate(
      ClientSession session, String userTranscript, TurnMetrics metrics) {
    TurnEvaluationCommand command =
        new TurnEvaluationCommand(
            session.getSituationType(),
            session.getFinalContext(),
            session.getSelectedOutfitId(),
            session.getConversationHistory(),
            session.getCurrentTurn(),
            session.getCurrentOpponentLine(),
            userTranscript,
            metrics);

    try {
      TurnEvaluationRawResult raw = turnEvaluationClient.evaluate(command);
      return new TurnEvaluationResult(raw.success(), raw.feedback(), false);
    } catch (RuntimeException exception) {
      // AI 실패는 전시 중단 사유가 아니므로(docs/prompt-and-rule-responsibility.md) 모든 런타임 실패를
      // 고정 fallback으로 흡수한다. 원인은 client/네트워크/파싱 등 다양해 특정 타입으로 좁힐 수 없다.
      log.warn("Turn evaluation AI call failed for session {}", session.getSessionId(), exception);
      return new TurnEvaluationResult(false, AI_FAILURE_FEEDBACK, true);
    }
  }

  private void validateTurnNo(ClientSession session, int turnNo) {
    if (session.getCurrentTurn() != turnNo) {
      throw new BusinessException(ErrorCode.SIMULATION_TURN_MISMATCH);
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
