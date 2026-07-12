package com.rehearsal.api.session.application;

import com.rehearsal.api.config.async.AsyncConfig;
import com.rehearsal.api.slot.application.ContextSlotExtractionService;
import com.rehearsal.api.slot.application.command.ExtractContextSlotsCommand;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.repository.SessionRepository;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Description("Worker that persists briefing/follow-up extraction results to session RDB rows")
@Component
@RequiredArgsConstructor
public class ContextExtractionWorker {

  private static final Logger log = LoggerFactory.getLogger(ContextExtractionWorker.class);

  private final SessionReader sessionReader;
  private final SessionRepository sessionRepository;
  private final ContextSlotExtractionService contextSlotExtractionService;

  @Async(AsyncConfig.CONTEXT_EXTRACTION_EXECUTOR)
  @Transactional
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void extractAsync(ContextExtractionRequested event) {
    if (event.mode() == SlotExtractionMode.INITIAL) {
      extractBriefing(event.sessionId(), event.transcript());
      return;
    }
    extractFollowUp(event.sessionId(), event.transcript());
  }

  public void extractBriefing(String sessionId, String transcript) {
    try {
      ClientSession session = sessionReader.get(sessionId);

      ExtractContextSlotsResult result =
          contextSlotExtractionService.extract(
              new ExtractContextSlotsCommand(
                  session.getSituationType().getKey(),
                  transcript,
                  session.getFollowUpAttempt(),
                  SlotExtractionMode.INITIAL,
                  Map.of(),
                  List.of()));
      SessionContext context = SessionContext.from(session.getSituationType(), result.context());

      completeOrRequireFollowUp(session, result, context);
    } catch (RuntimeException exception) {
      fail(sessionId, exception);
    }
  }

  public void extractFollowUp(String sessionId, String transcript) {
    try {
      ClientSession session = sessionReader.get(sessionId);
      SessionContext currentContext = currentContext(session);
      List<String> targetSlotKeys = missingRequiredSlotKeys(session, currentContext);

      ExtractContextSlotsResult result =
          contextSlotExtractionService.extract(
              new ExtractContextSlotsCommand(
                  session.getSituationType().getKey(),
                  transcript,
                  session.getFollowUpAttempt(),
                  SlotExtractionMode.FOLLOW_UP,
                  currentContext.valuesWithSituationType(),
                  targetSlotKeys));
      SessionContext mergedContext = currentContext.merge(result.context());

      completeOrRequireFollowUp(session, result, mergedContext);
    } catch (RuntimeException exception) {
      fail(sessionId, exception);
    }
  }

  private void completeOrRequireFollowUp(
      ClientSession session,
      ExtractContextSlotsResult result,
      SessionContext context) {
    sessionRepository.saveContext(session.getSessionId(), context);
    if (result.readyForSimulation()) {
      session.completeContext();
      sessionRepository.saveSession(session);
      return;
    }
    session.requireFollowUp();
    sessionRepository.saveSession(session);
  }

  private SessionContext currentContext(ClientSession session) {
    return sessionRepository
        .findContext(session.getSessionId())
        .orElseGet(() -> SessionContext.empty(session.getSituationType()));
  }

  private List<String> missingRequiredSlotKeys(
      ClientSession session, SessionContext currentContext) {
    ContextSlotSchemaType schema =
        ContextSlotSchemaType.findBySituationType(session.getSituationType())
            .orElseThrow(() -> new IllegalArgumentException("Unsupported situation type"));
    Map<String, Object> values = currentContext.values();
    return schema.getItems().stream()
        .filter(item -> item.requiredLevel() == RequiredLevel.REQUIRED)
        .map(item -> item.slotType().getKey())
        .filter(key -> isMissing(values.get(key)))
        .toList();
  }

  private boolean isMissing(Object value) {
    return value == null || value.toString().isBlank();
  }

  private void fail(String sessionId, RuntimeException exception) {
    log.error("Context extraction worker failed for session {}", sessionId, exception);
    ClientSession session = sessionReader.get(sessionId);
    session.failContext();
    sessionRepository.saveSession(session);
  }
}
