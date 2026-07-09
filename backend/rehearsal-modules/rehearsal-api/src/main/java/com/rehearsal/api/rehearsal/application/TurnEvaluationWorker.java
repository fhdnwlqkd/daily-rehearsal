package com.rehearsal.api.rehearsal.application;

import com.rehearsal.api.config.async.AsyncConfig;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationCommand;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationClient;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationJobStore;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Description("turn evaluation을 백그라운드 스레드에서 실행하고 결과를 job store에 기록하는 워커")
@Component
@RequiredArgsConstructor
public class TurnEvaluationWorker {

  private static final Logger log = LoggerFactory.getLogger(TurnEvaluationWorker.class);
  private static final String AI_FAILURE_FEEDBACK = "다시 시도해보세요.";

  private final SessionReader sessionReader;
  private final SessionCache sessionCache;
  private final TurnEvaluationClient turnEvaluationClient;
  private final TurnEvaluationJobStore turnEvaluationJobStore;

  @Async(AsyncConfig.TURN_EVALUATION_EXECUTOR)
  public void evaluateAsync(
      String sessionId, int turnNo, String userTranscript, TurnMetrics metrics) {
    try {
      // 제출 시점 이후 시간이 지났을 수 있으므로 세션을 다시 조회한다.
      ClientSession session = sessionReader.get(sessionId);
      TurnEvaluationResult result = evaluate(session, userTranscript, metrics);

      session.recordTurn(userTranscript, result.success(), result.feedback(), result.fallback());
      sessionCache.save(session);

      turnEvaluationJobStore.save(TurnEvaluationJob.pending(sessionId, turnNo).complete(result));
    } catch (RuntimeException exception) {
      // 세션 소실/상태 불일치/Redis 장애 등, 아래 evaluate()의 AI 호출 실패 fallback으로 흡수되지 않는
      // 예기치 못한 실패다. 여기서 FAILED로 기록하지 않으면 job이 영원히 PENDING으로 남아
      // polling 클라이언트가 무한 대기하게 된다.
      log.error(
          "Turn evaluation worker failed unexpectedly for session {} turn {}",
          sessionId,
          turnNo,
          exception);
      turnEvaluationJobStore.save(
          TurnEvaluationJob.pending(sessionId, turnNo).fail(exception.getMessage()));
    }
  }

  private TurnEvaluationResult evaluate(
      ClientSession session, String userTranscript, TurnMetrics metrics) {
    TurnEvaluationCommand command =
        new TurnEvaluationCommand(
            session.getSituationType(),
            session.getFinalContext().valuesWithSituationType(),
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
}
