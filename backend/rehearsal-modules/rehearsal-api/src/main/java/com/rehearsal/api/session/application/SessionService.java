package com.rehearsal.api.session.application;

import com.rehearsal.api.decart.application.OutfitSpecResolver;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.model.ContextExtractionJobType;
import com.rehearsal.domain.extraction.port.ContextExtractionJobStore;
import com.rehearsal.domain.extraction.usecase.GetContextExtractionUseCase;
import com.rehearsal.domain.extraction.usecase.SubmitContextExtractionUseCase;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.repository.SessionRepository;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Description(
    "Application service for P1 client session create, context extraction job, and outfit flow")
@Service
@RequiredArgsConstructor
public class SessionService
    implements CreateSessionUseCase,
        GetSessionUseCase,
        UpdateClientSessionUseCase,
        SubmitContextExtractionUseCase,
        GetContextExtractionUseCase {

  private static final String JOB_START_FAILURE_MESSAGE = "Context extraction job could not start.";

  private final SessionRepository sessionRepository;
  private final SessionReader sessionReader;
  private final OutfitSpecResolver outfitSpecResolver;
  private final ContextExtractionJobStore contextExtractionJobStore;
  private final ContextExtractionWorker contextExtractionWorker;

  @Override
  @Transactional
  public ClientSession createSession(SituationType situationType) {
    ClientSession session = ClientSession.create(situationType);
    return sessionRepository.saveSession(session);
  }

  @Override
  public ClientSession getSession(String sessionId) {
    return sessionReader.get(sessionId);
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
      ContextExtractionJob failedJob = job.fail(JOB_START_FAILURE_MESSAGE);
      contextExtractionJobStore.save(failedJob);
      return failedJob;
    }
    return job;
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
      ContextExtractionJob failedJob = job.fail(JOB_START_FAILURE_MESSAGE);
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
  @Transactional
  public ClientSession confirmOutfit(String sessionId, String selectedOutfitId) {
    ClientSession session = getSession(sessionId);

    outfitSpecResolver.resolve(selectedOutfitId);
    session.confirmOutfit(selectedOutfitId);
    return sessionRepository.saveSession(session);
  }
}
