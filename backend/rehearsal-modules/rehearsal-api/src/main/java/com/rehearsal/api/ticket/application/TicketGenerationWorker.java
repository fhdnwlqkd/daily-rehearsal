package com.rehearsal.api.ticket.application;

import com.rehearsal.api.config.async.AsyncConfig;
import com.rehearsal.api.config.ticket.TicketProperties;
import com.rehearsal.api.decart.application.OutfitSpecResolver;
import com.rehearsal.api.rehearsal.application.SimulationContextReader;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.model.VideoUploadStatus;
import com.rehearsal.domain.session.repository.SessionRepository;
import com.rehearsal.domain.slot.registry.ContextSlotOptionType;
import com.rehearsal.domain.slot.registry.ContextSlotType;
import com.rehearsal.domain.ticket.model.TicketCopyRawResult;
import com.rehearsal.domain.ticket.model.TicketCopyResult;
import com.rehearsal.domain.ticket.model.TicketGenerationCommand;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.model.TicketPayload;
import com.rehearsal.domain.ticket.model.TicketSnapshot;
import com.rehearsal.domain.ticket.port.TicketCopyGeneratorClient;
import com.rehearsal.domain.ticket.port.TicketJobStore;
import com.rehearsal.domain.ticket.registry.TicketCopyDefinition;
import com.rehearsal.domain.ticket.registry.TicketCopyRegistry;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Description("Generates the final change card in a background thread and records its result")
@Component
@RequiredArgsConstructor
public class TicketGenerationWorker {

  private static final Logger log = LoggerFactory.getLogger(TicketGenerationWorker.class);
  private static final String UNSPECIFIED_SNAPSHOT_LABEL = "따로 정하지 않음";

  private final SessionReader sessionReader;
  private final SessionRepository sessionRepository;
  private final SimulationContextReader simulationContextReader;
  private final OutfitSpecResolver outfitSpecResolver;
  private final TicketCopyGeneratorClient ticketCopyGeneratorClient;
  private final TicketJobStore ticketJobStore;
  private final TicketProperties ticketProperties;

  @Async(AsyncConfig.TICKET_GENERATION_EXECUTOR)
  public void generateAsync(String sessionId) {
    try {
      ClientSession session = sessionReader.get(sessionId);
      List<ConversationHistory> conversationHistory =
          simulationContextReader.history(sessionId, Integer.MAX_VALUE);
      List<TurnEvaluation> turnEvaluations = simulationContextReader.evaluations(sessionId);
      SessionContext context = contextOf(session);
      TicketCopyResult copy = generateCopy(session, context, conversationHistory, turnEvaluations);

      session.completeSimulation();
      sessionRepository.saveSession(session);

      TicketPayload payload = buildPayload(session, context, copy);
      ticketJobStore.save(TicketJob.pending(sessionId).complete(payload));
    } catch (RuntimeException exception) {
      log.error(
          "Ticket generation worker failed unexpectedly for session {}", sessionId, exception);
      ticketJobStore.save(TicketJob.pending(sessionId).fail(exception.getMessage()));
    }
  }

  private TicketCopyResult generateCopy(
      ClientSession session,
      SessionContext context,
      List<ConversationHistory> conversationHistory,
      List<TurnEvaluation> turnEvaluations) {
    TicketGenerationCommand command =
        new TicketGenerationCommand(
            session.getSituationType(),
            context.valuesWithSituationType(),
            session.getSelectedOutfitId(),
            conversationHistory,
            turnEvaluations);

    try {
      TicketCopyRawResult raw = ticketCopyGeneratorClient.generate(command);
      return new TicketCopyResult(raw.changeCard(), false);
    } catch (RuntimeException exception) {
      log.warn("Ticket copy AI call failed for session {}", session.getSessionId(), exception);
      return new TicketCopyResult(fallbackDefinition(session).fallbackChangeCard(), true);
    }
  }

  private TicketCopyDefinition fallbackDefinition(ClientSession session) {
    return TicketCopyRegistry.findByType(session.getSituationType())
        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  private TicketPayload buildPayload(
      ClientSession session, SessionContext context, TicketCopyResult copy) {
    boolean videoAvailable = session.getVideoUploadStatus() == VideoUploadStatus.COMPLETED;
    String downloadUrl = mobileDownloadUrl(session.getSessionId());

    return new TicketPayload(
        snapshotOf(session, context),
        copy.changeCard(),
        copy.fallback(),
        session.getVideoUrl(),
        videoAvailable,
        downloadUrl,
        downloadUrl);
  }

  private String mobileDownloadUrl(String sessionId) {
    String baseUrl = ticketProperties.getDownloadPageBaseUrl();
    String normalizedBaseUrl =
        baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return normalizedBaseUrl + "/download/" + sessionId;
  }

  private SessionContext contextOf(ClientSession session) {
    return sessionRepository
        .findContext(session.getSessionId())
        .orElseGet(() -> SessionContext.empty(session.getSituationType()));
  }

  private TicketSnapshot snapshotOf(ClientSession session, SessionContext context) {
    Map<String, Object> values = context.values();
    return new TicketSnapshot(
        session.getSituationType().getDisplayName(),
        stringValue(values, ContextSlotType.CRITICAL_MOMENT),
        desiredPersonaLabel(values),
        selectedOutfitLabel(session));
  }

  private String stringValue(Map<String, Object> values, ContextSlotType slotType) {
    Object value = values.get(slotType.getKey());
    if (value instanceof String text && !text.isBlank() && !isMissingPlaceholder(text)) {
      return text.strip();
    }
    return UNSPECIFIED_SNAPSHOT_LABEL;
  }

  private String desiredPersonaLabel(Map<String, Object> values) {
    Object value = values.get(ContextSlotType.DESIRED_PERSONA.getKey());
    if (value instanceof String optionKey) {
      return ContextSlotOptionType.findByKey(optionKey)
          .map(ContextSlotOptionType::getLabel)
          .orElse(UNSPECIFIED_SNAPSHOT_LABEL);
    }
    return UNSPECIFIED_SNAPSHOT_LABEL;
  }

  private boolean isMissingPlaceholder(String value) {
    return value.strip().endsWith("제공되지 않음");
  }

  private String selectedOutfitLabel(ClientSession session) {
    String selectedOutfitId = session.getSelectedOutfitId();
    if (selectedOutfitId == null || selectedOutfitId.isBlank()) {
      return "선택한 스타일";
    }
    return outfitSpecResolver.resolveLabel(selectedOutfitId);
  }
}
