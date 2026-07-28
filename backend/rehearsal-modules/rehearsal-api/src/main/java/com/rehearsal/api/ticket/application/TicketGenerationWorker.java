package com.rehearsal.api.ticket.application;

import com.rehearsal.api.config.async.AsyncConfig;
import com.rehearsal.api.config.ticket.TicketProperties;
import com.rehearsal.api.rehearsal.application.SimulationContextReader;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.VideoUploadStatus;
import com.rehearsal.domain.session.repository.SessionRepository;
import com.rehearsal.domain.ticket.model.TicketCopyRawResult;
import com.rehearsal.domain.ticket.model.TicketCopyResult;
import com.rehearsal.domain.ticket.model.TicketGenerationCommand;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.model.TicketPayload;
import com.rehearsal.domain.ticket.port.TicketCopyGeneratorClient;
import com.rehearsal.domain.ticket.port.TicketJobStore;
import com.rehearsal.domain.ticket.registry.TicketCopyDefinition;
import com.rehearsal.domain.ticket.registry.TicketCopyRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Description("티켓 카피 생성을 백그라운드 스레드에서 실행하고 결과를 job store에 기록하는 워커")
@Component
@RequiredArgsConstructor
public class TicketGenerationWorker {

  private static final Logger log = LoggerFactory.getLogger(TicketGenerationWorker.class);

  private final SessionReader sessionReader;
  private final SessionRepository sessionRepository;
  private final SimulationContextReader simulationContextReader;
  private final TicketCopyGeneratorClient ticketCopyGeneratorClient;
  private final TicketJobStore ticketJobStore;
  private final TicketProperties ticketProperties;

  @Async(AsyncConfig.TICKET_GENERATION_EXECUTOR)
  public void generateAsync(String sessionId) {
    try {
      // 제출 시점 이후 시간이 지났을 수 있으므로 세션을 다시 조회한다.
      ClientSession session = sessionReader.get(sessionId);
      List<ConversationHistory> conversationHistory =
          simulationContextReader.history(sessionId, Integer.MAX_VALUE);
      List<TurnEvaluation> turnEvaluations = simulationContextReader.evaluations(sessionId);
      TicketCopyResult copy = generateCopy(session, conversationHistory, turnEvaluations);

      session.completeSimulation();
      sessionRepository.saveSession(session);

      TicketPayload payload = buildPayload(session, copy, conversationHistory, turnEvaluations);
      ticketJobStore.save(TicketJob.pending(sessionId).complete(payload));
    } catch (RuntimeException exception) {
      // 세션 소실/상태 불일치/Redis 장애 등, 아래 generateCopy()의 AI 호출 실패 fallback으로 흡수되지 않는
      // 예기치 못한 실패다. 여기서 FAILED로 기록하지 않으면 job이 영원히 PENDING으로 남아
      // polling 클라이언트가 무한 대기하게 된다.
      log.error(
          "Ticket generation worker failed unexpectedly for session {}", sessionId, exception);
      ticketJobStore.save(TicketJob.pending(sessionId).fail(exception.getMessage()));
    }
  }

  private TicketCopyResult generateCopy(
      ClientSession session,
      List<ConversationHistory> conversationHistory,
      List<TurnEvaluation> turnEvaluations) {
    TicketGenerationCommand command =
        new TicketGenerationCommand(
            session.getSituationType(),
            simulationContextReader.context(session),
            session.getSelectedOutfitId(),
            conversationHistory,
            turnEvaluations);

    try {
      TicketCopyRawResult raw = ticketCopyGeneratorClient.generate(command);
      return new TicketCopyResult(raw.title(), raw.message(), false);
    } catch (RuntimeException exception) {
      // AI 실패는 전시 중단 사유가 아니므로 모든 런타임 실패를 상황 타입별 고정 fallback으로 흡수한다.
      log.warn("Ticket copy AI call failed for session {}", session.getSessionId(), exception);
      TicketCopyDefinition fallback = fallbackDefinition(session);
      return new TicketCopyResult(fallback.fallbackTitle(), fallback.fallbackMessage(), true);
    }
  }

  private TicketCopyDefinition fallbackDefinition(ClientSession session) {
    return TicketCopyRegistry.findByType(session.getSituationType())
        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  private TicketPayload buildPayload(
      ClientSession session,
      TicketCopyResult copy,
      List<ConversationHistory> conversationHistory,
      List<TurnEvaluation> turnEvaluations) {
    boolean videoAvailable = session.getVideoUploadStatus() == VideoUploadStatus.COMPLETED;
    String downloadUrl =
        videoAvailable ? session.getVideoUrl() : ticketProperties.getDownloadFallbackUrl();

    return new TicketPayload(
        copy.title(),
        copy.message(),
        copy.fallback(),
        session.getSituationType(),
        session.getSelectedOutfitId(),
        conversationHistory,
        turnEvaluations,
        session.getVideoUrl(),
        videoAvailable,
        downloadUrl,
        downloadUrl);
  }
}
