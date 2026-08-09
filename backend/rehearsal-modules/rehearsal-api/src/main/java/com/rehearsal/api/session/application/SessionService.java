package com.rehearsal.api.session.application;

import com.rehearsal.api.decart.application.OutfitSpecResolver;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.extraction.usecase.GetContextExtractionUseCase;
import com.rehearsal.domain.extraction.usecase.SubmitContextExtractionUseCase;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextCollectionState;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.repository.SessionRepository;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Description("Application service for P1 session, RDB context polling, and outfit flow")
@Service
@RequiredArgsConstructor
public class SessionService
    implements CreateSessionUseCase,
        GetSessionUseCase,
        UpdateClientSessionUseCase,
        SubmitContextExtractionUseCase,
        GetContextExtractionUseCase {

  private final SessionRepository sessionRepository;
  private final SessionReader sessionReader;
  private final OutfitSpecResolver outfitSpecResolver;
  private final ApplicationEventPublisher eventPublisher;

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
  @Transactional
  public ClientSession submitBriefingExtraction(String sessionId, String transcript) {
    ClientSession session = getSession(sessionId);
    session.startContextExtraction();
    ClientSession saved = sessionRepository.saveSession(session);
    eventPublisher.publishEvent(
        new ContextExtractionRequested(sessionId, transcript, SlotExtractionMode.INITIAL));
    return saved;
  }

  @Override
  @Transactional
  public ClientSession submitFollowUpExtraction(String sessionId, String transcript) {
    ClientSession session = getSession(sessionId);
    session.startFollowUpMerge();
    ClientSession saved = sessionRepository.saveSession(session);
    eventPublisher.publishEvent(
        new ContextExtractionRequested(sessionId, transcript, SlotExtractionMode.FOLLOW_UP));
    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public ContextCollectionState getContext(String sessionId) {
    ClientSession session = getSession(sessionId);
    SessionContext context =
        sessionRepository
            .findContext(sessionId)
            .orElseGet(() -> SessionContext.empty(session.getSituationType()));
    List<String> missingSlotKeys = missingRequiredSlotKeys(session, context);
    List<String> followUpQuestions = followUpQuestions(session, missingSlotKeys);
    return new ContextCollectionState(
        sessionId, session.getContextStatus(), context, missingSlotKeys, followUpQuestions);
  }

  @Override
  @Transactional
  public ClientSession confirmOutfit(String sessionId, String selectedOutfitId) {
    ClientSession session = getSession(sessionId);

    outfitSpecResolver.resolve(selectedOutfitId);
    session.confirmOutfit(selectedOutfitId);
    return sessionRepository.saveSession(session);
  }

  private List<String> missingRequiredSlotKeys(ClientSession session, SessionContext context) {
    if (session.getContextStatus() != ContextStatus.FOLLOW_UP_REQUIRED) {
      return List.of();
    }
    ContextSlotSchemaType schema = schema(session);
    return schema.getItems().stream()
        .filter(item -> item.requiredLevel() == RequiredLevel.REQUIRED)
        .map(item -> item.slotType().getKey())
        .filter(key -> isMissing(context.values().get(key)))
        .toList();
  }

  private List<String> followUpQuestions(ClientSession session, List<String> missingSlotKeys) {
    if (missingSlotKeys.isEmpty()) {
      return List.of();
    }
    return schema(session).getItems().stream()
        .filter(item -> missingSlotKeys.contains(item.slotType().getKey()))
        .map(item -> item.slotType().getFollowUpHint())
        .filter(question -> question != null && !question.isBlank())
        .toList();
  }

  private ContextSlotSchemaType schema(ClientSession session) {
    return ContextSlotSchemaType.findBySituationType(session.getSituationType())
        .orElseThrow(() -> new IllegalArgumentException("Unsupported situation type"));
  }

  private boolean isMissing(Object value) {
    return value == null || value.toString().isBlank();
  }
}
