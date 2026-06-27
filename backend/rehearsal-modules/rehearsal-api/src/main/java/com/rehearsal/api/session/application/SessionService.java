package com.rehearsal.api.session.application;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.session.usecase.command.CompleteSessionContextCommand;
import com.rehearsal.domain.session.usecase.command.UpdateBriefingTranscriptCommand;
import com.rehearsal.domain.session.usecase.command.UpdateFeedbackResultCommand;
import com.rehearsal.domain.session.usecase.command.UpdateFinalResultCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSelectedOutfitCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSessionContextCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSimulationDraftCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionService
    implements CreateSessionUseCase, GetSessionUseCase, UpdateClientSessionUseCase {

  private final SessionCache sessionCache;

  @Override
  public ClientSession createSession(String channel) {
    ClientSession session = ClientSession.create(channel);
    return sessionCache.save(session);
  }

  @Override
  public ClientSession getSession(String sessionId) {
    return sessionCache
        .findById(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
  }

  @Override
  public ClientSession updateBriefingTranscript(UpdateBriefingTranscriptCommand command) {
    ClientSession session = getSession(command.sessionId());
    session.updateBriefingTranscript(command.briefingTranscript());
    session.updateStatus(SessionStatus.CONTEXT_EXTRACTING);
    session.updateContextStatus(ContextStatus.EXTRACTING);
    return sessionCache.save(session);
  }

  @Override
  public ClientSession updateContext(UpdateSessionContextCommand command) {
    ClientSession session = getSession(command.sessionId());
    session.updateContext(
        command.partialContext(), command.missingRequiredSlotKeys(), command.followUpQuestion());
    if (hasMissingRequiredSlots(command.missingRequiredSlotKeys())) {
      session.updateStatus(SessionStatus.FOLLOW_UP_REQUIRED);
      session.updateContextStatus(ContextStatus.FOLLOW_UP_REQUIRED);
      session.increaseFollowUpAttempt();
    } else {
      session.updateContextStatus(ContextStatus.EXTRACTING);
    }
    return sessionCache.save(session);
  }

  @Override
  public ClientSession completeContext(CompleteSessionContextCommand command) {
    ClientSession session = getSession(command.sessionId());
    session.updateFinalUserContext(command.finalUserContext());
    session.updateStatus(SessionStatus.TRANSFORMATION_READY);
    session.updateContextStatus(ContextStatus.COMPLETED);
    return sessionCache.save(session);
  }

  @Override
  public ClientSession updateSelectedOutfit(UpdateSelectedOutfitCommand command) {
    ClientSession session = getSession(command.sessionId());
    session.updateSelectedOutfitId(command.selectedOutfitId());
    session.updateStatus(SessionStatus.REHEARSAL_READY);
    return sessionCache.save(session);
  }

  @Override
  public ClientSession updateSimulationDraft(UpdateSimulationDraftCommand command) {
    ClientSession session = getSession(command.sessionId());
    session.updateSimulationDraft(command.simulationDraft());
    session.updateStatus(SessionStatus.REHEARSAL_READY);
    return sessionCache.save(session);
  }

  @Override
  public ClientSession updateFeedbackResult(UpdateFeedbackResultCommand command) {
    ClientSession session = getSession(command.sessionId());
    session.updateFeedbackResult(command.feedbackResult());
    session.updateStatus(SessionStatus.RESULT_READY);
    return sessionCache.save(session);
  }

  @Override
  public ClientSession updateFinalResult(UpdateFinalResultCommand command) {
    ClientSession session = getSession(command.sessionId());
    session.updateFinalResult(command.finalResult());
    session.updateStatus(SessionStatus.COMPLETED);
    return sessionCache.save(session);
  }

  private boolean hasMissingRequiredSlots(List<String> missingRequiredSlotKeys) {
    return missingRequiredSlotKeys != null && !missingRequiredSlotKeys.isEmpty();
  }
}
