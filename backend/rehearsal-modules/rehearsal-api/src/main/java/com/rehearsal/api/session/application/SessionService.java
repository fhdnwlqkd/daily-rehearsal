package com.rehearsal.api.session.application;

import com.rehearsal.api.decart.application.OutfitSpecResolver;
import com.rehearsal.api.slot.application.ContextSlotExtractionService;
import com.rehearsal.api.slot.application.command.ExtractContextSlotsCommand;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Description(
    "Application service for P1 client session create, get, briefing submit, and outfit confirm flow")
@Service
@RequiredArgsConstructor
public class SessionService
    implements CreateSessionUseCase, GetSessionUseCase, UpdateClientSessionUseCase {

  private final SessionCache sessionCache;
  private final SessionReader sessionReader;
  private final OutfitSpecResolver outfitSpecResolver;
  private final ContextSlotExtractionService contextSlotExtractionService;

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
    Map<String, Object> context = contextWithSituationType(session, result.context());

    completeOrRequireFollowUp(session, result, context);
    return sessionCache.save(session);
  }

  @Override
  public ClientSession submitFollowUp(String sessionId, String transcript) {
    ClientSession session = getSession(sessionId);
    Map<String, Object> currentContext = safeContext(session.getPartialContext());
    List<String> targetSlotKeys = session.getMissingSlotKeys();

    session.startFollowUpMerge();
    ExtractContextSlotsResult result =
        contextSlotExtractionService.extract(
            new ExtractContextSlotsCommand(
                session.getSituationType().key(),
                transcript,
                session.getFollowUpAttempt(),
                SlotExtractionMode.FOLLOW_UP,
                currentContext,
                targetSlotKeys));
    Map<String, Object> mergedContext =
        contextWithSituationType(session, mergeContext(currentContext, result.context()));

    completeOrRequireFollowUp(session, result, mergedContext);
    return sessionCache.save(session);
  }

  @Override
  public ClientSession confirmOutfit(String sessionId, String selectedOutfitId) {
    ClientSession session = getSession(sessionId);

    outfitSpecResolver.resolve(selectedOutfitId);
    session.confirmOutfit(selectedOutfitId);
    return sessionCache.save(session);
  }

  private Map<String, Object> contextWithSituationType(
      ClientSession session, Map<String, Object> extractedContext) {
    Map<String, Object> context = new LinkedHashMap<>();
    if (extractedContext != null) {
      context.putAll(extractedContext);
    }
    context.put("situation_type", session.getSituationType().key());
    return context;
  }

  private Map<String, Object> mergeContext(
      Map<String, Object> currentContext, Map<String, Object> extractedContext) {
    Map<String, Object> context = new LinkedHashMap<>();
    if (currentContext != null) {
      context.putAll(currentContext);
    }
    if (extractedContext != null) {
      context.putAll(extractedContext);
    }
    return context;
  }

  private Map<String, Object> safeContext(Map<String, Object> context) {
    return context == null ? Map.of() : new LinkedHashMap<>(context);
  }

  private void completeOrRequireFollowUp(
      ClientSession session, ExtractContextSlotsResult result, Map<String, Object> context) {
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
