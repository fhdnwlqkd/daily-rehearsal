package com.rehearsal.api.session.application;

import com.rehearsal.api.decart.application.OutfitSpecResolver;
import com.rehearsal.api.slot.application.ContextSlotExtractionService;
import com.rehearsal.api.slot.application.command.ExtractContextSlotsCommand;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.model.ContextExtractionJobType;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.extraction.port.ContextExtractionJobStore;
import com.rehearsal.domain.extraction.usecase.GetContextExtractionUseCase;
import com.rehearsal.domain.extraction.usecase.SubmitContextExtractionUseCase;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Description(
    "Application service for P1 client session create, get, briefing submit, and outfit confirm flow")
@Service
@RequiredArgsConstructor
public class SessionService
    implements CreateSessionUseCase,
        GetSessionUseCase,
        UpdateClientSessionUseCase,
        SubmitContextExtractionUseCase,
        GetContextExtractionUseCase {

  private final SessionCache sessionCache;
  private final SessionReader sessionReader;
  private final OutfitSpecResolver outfitSpecResolver;
  private final ContextSlotExtractionService contextSlotExtractionService;
  private final ContextExtractionJobStore contextExtractionJobStore;
  private final ContextExtractionWorker contextExtractionWorker;

  @Override
  public ClientSession createSession(SituationType situationType) {
    ClientSession session = ClientSession.create(situationType);
    return sessionCache.save(session);
  }

  @Override
  public ClientSession getSession(String sessionId) {
    return sessionReader.get(sessionId);
  }

  @Override
  public ClientSession submitBriefing(String sessionId, String transcript) {
    ClientSession session = getSession(sessionId);

    session.startContextExtraction();
    ExtractContextSlotsResult result =
        contextSlotExtractionService.extract(
            new ExtractContextSlotsCommand(
                session.getSituationType().key(),
                transcript,
                session.getFollowUpAttempt(),
                SlotExtractionMode.INITIAL,
                Map.of(),
                List.of()));
    SessionContext context = SessionContext.from(session.getSituationType(), result.context());

    completeOrRequireFollowUp(session, result, context);
    return sessionCache.save(session);
  }

  @Override
  public ContextExtractionJob submitBriefingExtraction(String sessionId, String transcript) {
    ClientSession session = getSession(sessionId);
    ContextExtractionJob job =
        ContextExtractionJob.pending(
            session.getSessionId(), session.getSituationType(), ContextExtractionJobType.BRIEFING);
    contextExtractionJobStore.save(job);
    try {
      contextExtractionWorker.extractBriefingAsync(job, transcript);
    } catch (RuntimeException exception) {
      ContextExtractionJob failedJob = job.fail("작업을 시작하지 못했습니다.");
      contextExtractionJobStore.save(failedJob);
      return failedJob;
    }
    return job;
  }

  @Override
  public ClientSession submitFollowUp(String sessionId, String transcript) {
    ClientSession session = getSession(sessionId);
    SessionContext currentContext = safeContext(session);
    List<String> targetSlotKeys = session.getMissingSlotKeys();

    session.startFollowUpMerge();
    ExtractContextSlotsResult result =
        contextSlotExtractionService.extract(
            new ExtractContextSlotsCommand(
                session.getSituationType().key(),
                transcript,
                session.getFollowUpAttempt(),
                SlotExtractionMode.FOLLOW_UP,
                currentContext.valuesWithSituationType(),
                targetSlotKeys));
    SessionContext mergedContext = currentContext.merge(result.context());

    completeOrRequireFollowUp(session, result, mergedContext);
    return sessionCache.save(session);
  }

  @Override
  public ContextExtractionJob submitFollowUpExtraction(String sessionId, String transcript) {
    ClientSession session = getSession(sessionId);
    ContextExtractionJob job =
        ContextExtractionJob.pending(
            session.getSessionId(), session.getSituationType(), ContextExtractionJobType.FOLLOW_UP);
    contextExtractionJobStore.save(job);
    try {
      contextExtractionWorker.extractFollowUpAsync(job, transcript);
    } catch (RuntimeException exception) {
      ContextExtractionJob failedJob = job.fail("작업을 시작하지 못했습니다.");
      contextExtractionJobStore.save(failedJob);
      return failedJob;
    }
    return job;
  }

  @Override
  public ContextExtractionJob get(String sessionId, String jobId) {
    return contextExtractionJobStore
        .findById(sessionId, jobId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONTEXT_EXTRACTION_JOB_NOT_FOUND));
  }

  @Override
  public ClientSession confirmOutfit(String sessionId, String selectedOutfitId) {
    ClientSession session = getSession(sessionId);

    outfitSpecResolver.resolve(selectedOutfitId);
    session.confirmOutfit(selectedOutfitId);
    return sessionCache.save(session);
  }

  private SessionContext safeContext(ClientSession session) {
    return session.getPartialContext() == null
        ? SessionContext.empty(session.getSituationType())
        : session.getPartialContext();
  }

  private void completeOrRequireFollowUp(
      ClientSession session, ExtractContextSlotsResult result, SessionContext context) {
    if (result.readyForSimulation()) {
      session.completeContext(context);
      return;
    }
    session.requireFollowUp(context, result.missingRequiredSlotKeys(), followUpQuestions(result));
  }

  private List<String> followUpQuestions(ExtractContextSlotsResult result) {
    if (result.followUpQuestion() == null || result.followUpQuestion().isBlank()) {
      return List.of();
    }
    return List.of(result.followUpQuestion());
  }
}
