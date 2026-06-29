package com.rehearsal.api.decart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.api.config.decart.DecartProperties;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.decart.usecase.result.DecartSpecResult;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DecartSpecServiceTest {

  private static final String OUTFIT_ID = "presentation_jacket_01";
  private static final String CLIENT_TOKEN = "test-client-token";

  @Test
  void returnsDecartSpecResultForValidSession() {
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    DecartSpecService service = serviceWith(session);

    DecartSpecResult result = service.getDecartSpec(session.getSessionId(), OUTFIT_ID);

    assertThat(result.clientToken()).isEqualTo(CLIENT_TOKEN);
    assertThat(result.spec().model()).isEqualTo("lucy-vton-latest");
    assertThat(result.spec().prompt()).isNotBlank();
    assertThat(result.spec().referenceImageUrl()).isNotBlank();
  }

  @Test
  void updatesSelectedOutfitIdOnSession() {
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    DecartSpecService service = serviceWith(session);

    service.getDecartSpec(session.getSessionId(), OUTFIT_ID);

    assertThat(session.getSelectedOutfitId()).isEqualTo(OUTFIT_ID);
  }

  @Test
  void throwsSessionNotFoundForUnknownSession() {
    DecartSpecService service =
        new DecartSpecService(new EmptySessionCache(), () -> CLIENT_TOKEN, resolverWithOutfit());

    assertThatThrownBy(() -> service.getDecartSpec("unknown-session-id", OUTFIT_ID))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
  }

  @Test
  void throwsInvalidSessionStateWhenNotTransformationReady() {
    ClientSession session = sessionWith(SessionStatus.BRIEFING);
    DecartSpecService service = serviceWith(session);

    assertThatThrownBy(() -> service.getDecartSpec(session.getSessionId(), OUTFIT_ID))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void throwsNotFoundForUnknownOutfitId() {
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    DecartSpecService service = serviceWith(session);

    assertThatThrownBy(() -> service.getDecartSpec(session.getSessionId(), "unknown_outfit"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  private DecartSpecService serviceWith(ClientSession session) {
    return new DecartSpecService(
        new InMemorySessionCache(session), () -> CLIENT_TOKEN, resolverWithOutfit());
  }

  private OutfitSpecResolver resolverWithOutfit() {
    DecartProperties properties = new DecartProperties();
    properties.setModel("lucy-vton-latest");

    DecartProperties.OutfitSpec outfitSpec = new DecartProperties.OutfitSpec();
    outfitSpec.setPrompt("Substitute the current top with a navy blue blazer");
    outfitSpec.setReferenceImageUrl("https://asset-store/outfits/jacket_01.png");
    outfitSpec.setEnhance(false);
    properties.getOutfits().put(OUTFIT_ID, outfitSpec);

    return new OutfitSpecResolver(properties);
  }

  private ClientSession sessionWith(SessionStatus status) {
    ClientSession session = ClientSession.create(ClientSession.DEFAULT_CHANNEL);
    session.updateStatus(status);
    return session;
  }

  static class EmptySessionCache implements SessionCache {

    @Override
    public ClientSession save(ClientSession session) {
      return session;
    }

    @Override
    public Optional<ClientSession> findById(String sessionId) {
      return Optional.empty();
    }
  }

  static class InMemorySessionCache implements SessionCache {

    private final Map<String, ClientSession> store = new HashMap<>();

    InMemorySessionCache(ClientSession session) {
      store.put(session.getSessionId(), session);
    }

    @Override
    public ClientSession save(ClientSession session) {
      store.put(session.getSessionId(), session);
      return session;
    }

    @Override
    public Optional<ClientSession> findById(String sessionId) {
      return Optional.ofNullable(store.get(sessionId));
    }
  }
}
