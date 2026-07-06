package com.rehearsal.api.session.application;

import com.rehearsal.api.decart.application.OutfitSpecResolver;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
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
    return sessionCache.save(session);
  }

  @Override
  public ClientSession confirmOutfit(String sessionId, String selectedOutfitId) {
    ClientSession session = getSession(sessionId);

    outfitSpecResolver.resolve(selectedOutfitId);
    session.confirmOutfit(selectedOutfitId);
    return sessionCache.save(session);
  }
}
