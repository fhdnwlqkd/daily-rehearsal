package com.rehearsal.api.session.application;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.session.usecase.command.CompleteSessionContextCommand;
import com.rehearsal.domain.session.usecase.command.CreateSessionCommand;
import com.rehearsal.domain.session.usecase.command.GetSessionCommand;
import com.rehearsal.domain.session.usecase.command.UpdateBriefingTranscriptCommand;
import com.rehearsal.domain.session.usecase.command.UpdateFeedbackResultCommand;
import com.rehearsal.domain.session.usecase.command.UpdateFinalResultCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSelectedOutfitCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSessionContextCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSimulationDraftCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionService
    implements CreateSessionUseCase, GetSessionUseCase, UpdateClientSessionUseCase {

  private final SessionCache sessionCache;

  @Override
  public ClientSession createSession(CreateSessionCommand command) {
    ClientSession session = ClientSession.create(command.channel());
    return sessionCache.save(session);
  }

  @Override
  public ClientSession getSession(GetSessionCommand command) {
    return sessionCache
        .findById(command.sessionId())
        .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
  }

  @Override
  public ClientSession updateBriefingTranscript(UpdateBriefingTranscriptCommand command) {
    ClientSession session =
        getSession(new GetSessionCommand(command.sessionId()))
            .updateBriefingTranscript(command.briefingTranscript());
    return sessionCache.save(session);
  }

  @Override
  public ClientSession updateContext(UpdateSessionContextCommand command) {
    ClientSession session =
        getSession(new GetSessionCommand(command.sessionId()))
            .updateContext(
                command.partialContext(),
                command.missingRequiredSlotKeys(),
                command.followUpQuestion());
    return sessionCache.save(session);
  }

  @Override
  public ClientSession completeContext(CompleteSessionContextCommand command) {
    ClientSession session =
        getSession(new GetSessionCommand(command.sessionId()))
            .completeContext(command.finalUserContext());
    return sessionCache.save(session);
  }

  @Override
  public ClientSession updateSelectedOutfit(UpdateSelectedOutfitCommand command) {
    ClientSession session =
        getSession(new GetSessionCommand(command.sessionId()))
            .updateSelectedOutfit(command.selectedOutfitId());
    return sessionCache.save(session);
  }

  @Override
  public ClientSession updateSimulationDraft(UpdateSimulationDraftCommand command) {
    ClientSession session =
        getSession(new GetSessionCommand(command.sessionId()))
            .updateSimulationDraft(command.simulationDraft());
    return sessionCache.save(session);
  }

  @Override
  public ClientSession updateFeedbackResult(UpdateFeedbackResultCommand command) {
    ClientSession session =
        getSession(new GetSessionCommand(command.sessionId()))
            .updateFeedbackResult(command.feedbackResult());
    return sessionCache.save(session);
  }

  @Override
  public ClientSession updateFinalResult(UpdateFinalResultCommand command) {
    ClientSession session =
        getSession(new GetSessionCommand(command.sessionId()))
            .updateFinalResult(command.finalResult());
    return sessionCache.save(session);
  }
}
