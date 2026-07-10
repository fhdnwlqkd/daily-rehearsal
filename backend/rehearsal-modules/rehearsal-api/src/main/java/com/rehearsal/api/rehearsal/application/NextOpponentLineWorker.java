package com.rehearsal.api.rehearsal.application;

import com.rehearsal.api.config.async.AsyncConfig;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import com.rehearsal.domain.rehearsal.model.OpponentLineJob;
import com.rehearsal.domain.rehearsal.model.OpponentLineResult;
import com.rehearsal.domain.rehearsal.port.OpponentLineGeneratorClient;
import com.rehearsal.domain.rehearsal.port.OpponentLineJobStore;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigRegistry;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Description("next opponent line 생성을 백그라운드 스레드에서 실행하고 결과를 job store에 기록하는 워커")
@Component
@RequiredArgsConstructor
public class NextOpponentLineWorker {

  private static final Logger log = LoggerFactory.getLogger(NextOpponentLineWorker.class);

  private final SessionReader sessionReader;
  private final SessionCache sessionCache;
  private final OpponentLineGeneratorClient opponentLineGeneratorClient;
  private final OpponentLineJobStore opponentLineJobStore;

  @Async(AsyncConfig.NEXT_LINE_EXECUTOR)
  public void generateAsync(String sessionId, int turnNo) {
    try {
      // 제출 시점 이후 시간이 지났을 수 있으므로 세션을 다시 조회한다.
      ClientSession session = sessionReader.get(sessionId);
      OpponentLineResult result = generate(session);

      session.updateOpponentLine(result.opponentLine());
      sessionCache.save(session);

      opponentLineJobStore.save(OpponentLineJob.pending(sessionId, turnNo).complete(result));
    } catch (RuntimeException exception) {
      // 세션 소실/상태 불일치/Redis 장애 등, 아래 generate()의 AI 호출 실패 fallback으로 흡수되지 않는
      // 예기치 못한 실패다. 여기서 FAILED로 기록하지 않으면 job이 영원히 PENDING으로 남아
      // polling 클라이언트가 무한 대기하게 된다.
      log.error(
          "Next opponent line worker failed unexpectedly for session {} turn {}",
          sessionId,
          turnNo,
          exception);
      opponentLineJobStore.save(
          OpponentLineJob.pending(sessionId, turnNo).fail(exception.getMessage()));
    }
  }

  private OpponentLineResult generate(ClientSession session) {
    OpponentLineCommand command =
        new OpponentLineCommand(
            session.getSituationType(),
            session.getFinalContext().valuesWithSituationType(),
            session.getSelectedOutfitId(),
            session.getConversationHistory(),
            session.getCurrentTurn());

    try {
      String opponentLine = opponentLineGeneratorClient.generate(command);
      return new OpponentLineResult(opponentLine, false);
    } catch (RuntimeException exception) {
      // AI 실패는 전시 중단 사유가 아니므로(docs/prompt-and-rule-responsibility.md) 모든 런타임 실패를
      // 타입별 고정 fallback으로 흡수한다. 원인은 client/네트워크/파싱 등 다양해 특정 타입으로 좁힐 수 없다.
      log.warn(
          "Next opponent line AI call failed for session {}", session.getSessionId(), exception);
      return new OpponentLineResult(fallbackLine(session), true);
    }
  }

  private String fallbackLine(ClientSession session) {
    RehearsalConfigDefinition config =
        RehearsalConfigRegistry.findByType(session.getSituationType())
            .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
    return config.nextLineFallback();
  }
}
