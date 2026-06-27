package com.rehearsal.api.session.application;

import com.rehearsal.api.session.contract.SessionContract.CreateSessionCommand;
import com.rehearsal.api.session.contract.SessionContract.CreateSessionResult;
import com.rehearsal.api.session.contract.SessionContract.GetSessionResult;
import com.rehearsal.api.session.contract.SessionContract.SubmitBriefingCommand;
import com.rehearsal.api.session.contract.SessionContract.SubmitFollowUpCommand;
import com.rehearsal.api.session.contract.SessionStatus;
import com.rehearsal.api.slot.application.ContextSlotExtractionService;
import com.rehearsal.api.slot.application.command.ExtractContextSlotsCommand;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

  private final InMemorySessionStore sessionStore;
  private final ContextSlotExtractionService contextSlotExtractionService;

  public SessionService(
      InMemorySessionStore sessionStore,
      ContextSlotExtractionService contextSlotExtractionService) {
    this.sessionStore = sessionStore;
    this.contextSlotExtractionService = contextSlotExtractionService;
  }

  public CreateSessionResult create(CreateSessionCommand command) {
    SessionState sessionState = SessionState.create(command.channel());
    sessionStore.save(sessionState);
    return CreateSessionResult.from(sessionState);
  }

  public GetSessionResult get(String sessionId) {
    return GetSessionResult.from(findSessionOrThrow(sessionId));
  }

  public GetSessionResult submitBriefing(SubmitBriefingCommand command) {
    SessionState session = findSessionOrThrow(command.sessionId());
    validateStatus(session, SessionStatus.BRIEFING);

    ExtractContextSlotsResult result =
        contextSlotExtractionService.extract(
            new ExtractContextSlotsCommand(
                ExtractContextSlotsCommand.DEFAULT_SCHEMA_KEY,
                command.transcript(),
                0,
                SlotExtractionMode.INITIAL,
                Map.of(),
                List.of()));

    session.applyInitialBriefing(command.transcript(), result);
    sessionStore.save(session);
    return GetSessionResult.from(session);
  }

  public GetSessionResult submitFollowUp(SubmitFollowUpCommand command) {
    SessionState session = findSessionOrThrow(command.sessionId());
    validateStatus(session, SessionStatus.FOLLOW_UP_REQUIRED);

    ExtractContextSlotsResult result =
        contextSlotExtractionService.extract(
            new ExtractContextSlotsCommand(
                ExtractContextSlotsCommand.DEFAULT_SCHEMA_KEY,
                command.transcript(),
                session.getFollowUpAttempt(),
                SlotExtractionMode.FOLLOW_UP,
                session.getPartialContext(),
                session.getMissingRequiredSlotKeys()));

    session.applyFollowUpBriefing(command.transcript(), result);
    sessionStore.save(session);
    return GetSessionResult.from(session);
  }

  private SessionState findSessionOrThrow(String sessionId) {
    return sessionStore
        .findById(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
  }

  private void validateStatus(SessionState session, SessionStatus expected) {
    if (session.getStatus() != expected) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
    }
  }
}
