package com.rehearsal.api.session.application;

import com.rehearsal.api.config.async.AsyncConfig;
import com.rehearsal.api.slot.application.ContextSlotExtractionService;
import com.rehearsal.api.slot.application.command.ExtractContextSlotsCommand;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.extraction.port.ContextExtractionJobStore;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Description("Worker that runs briefing/follow-up context extraction and records job results")
@Component
@RequiredArgsConstructor
public class ContextExtractionWorker {

  private static final Logger log = LoggerFactory.getLogger(ContextExtractionWorker.class);

  private final SessionReader sessionReader;
  private final SessionCache sessionCache;
  private final ContextSlotExtractionService contextSlotExtractionService;
  private final ContextExtractionJobStore contextExtractionJobStore;

  @Async(AsyncConfig.CONTEXT_EXTRACTION_EXECUTOR)
  public void extractBriefingAsync(ContextExtractionJob job, String transcript) {
    try {
      ClientSession session = sessionReader.get(job.sessionId());
      session.startContextExtraction();

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

      completeOrRequireFollowUp(session, job, result, context);
    } catch (RuntimeException exception) {
      fail(job, exception);
    }
  }

  @Async(AsyncConfig.CONTEXT_EXTRACTION_EXECUTOR)
  public void extractFollowUpAsync(ContextExtractionJob job, String transcript) {
    try {
      ClientSession session = sessionReader.get(job.sessionId());
      SessionContext currentContext = safeContext(session);
      List<String> targetSlotKeys = session.getMissingSlotKeys();

      session.startFollowUpMerge();
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

      completeOrRequireFollowUp(session, job, result, mergedContext);
    } catch (RuntimeException exception) {
      fail(job, exception);
    }
  }

  private void completeOrRequireFollowUp(
      ClientSession session,
      ContextExtractionJob job,
      ExtractContextSlotsResult result,
      SessionContext context) {
    if (result.readyForSimulation()) {
      session.completeContext(context);
      sessionCache.save(session);
      contextExtractionJobStore.save(job.completeWithFinalContext(context));
      return;
    }
    List<String> followUpQuestions = followUpQuestions(result);
    session.requireFollowUp(context, result.missingRequiredSlotKeys(), followUpQuestions);
    sessionCache.save(session);
    contextExtractionJobStore.save(job.completeWithFollowUpQuestions(followUpQuestions));
  }

  private SessionContext safeContext(ClientSession session) {
    return session.getPartialContext() == null
        ? SessionContext.empty(session.getSituationType())
        : session.getPartialContext();
  }

  private List<String> followUpQuestions(ExtractContextSlotsResult result) {
    if (result.followUpQuestion() == null || result.followUpQuestion().isBlank()) {
      return List.of();
    }
    return List.of(result.followUpQuestion());
  }

  private void fail(ContextExtractionJob job, RuntimeException exception) {
    log.error("Context extraction worker failed for session {}", job.sessionId(), exception);
    contextExtractionJobStore.save(job.fail(exception.getMessage()));
  }
}
