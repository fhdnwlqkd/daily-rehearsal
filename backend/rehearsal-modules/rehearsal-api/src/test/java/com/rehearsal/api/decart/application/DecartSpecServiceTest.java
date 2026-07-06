package com.rehearsal.api.decart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.api.config.decart.DecartProperties;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.InMemorySessionCache;
import com.rehearsal.api.support.TestClientSessions;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.decart.model.DecartSpec;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import org.junit.jupiter.api.Test;

class DecartSpecServiceTest {

  private static final String OUTFIT_ID = "presentation_jacket_01";
  private static final String CLIENT_TOKEN = "test-client-token";

  @Test
  void issuesDecartTokenForValidSession() {
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    DecartSpecService service = serviceWith(session);

    String token = service.issueDecartToken(session.getSessionId());

    assertThat(token).isEqualTo(CLIENT_TOKEN);
  }

  @Test
  void issueTokenThrowsSessionNotFound() {
    DecartSpecService service = serviceWith(new InMemorySessionCache());

    assertThatThrownBy(() -> service.issueDecartToken("unknown-session-id"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
  }

  @Test
  void issueTokenThrowsInvalidSessionStateWhenNotTransformationReady() {
    ClientSession session = sessionWith(SessionStatus.BRIEFING);
    DecartSpecService service = serviceWith(session);

    assertThatThrownBy(() -> service.issueDecartToken(session.getSessionId()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void returnsOutfitSpecForValidSession() {
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    DecartSpecService service = serviceWith(session);

    DecartSpec spec = service.getOutfitSpec(session.getSessionId(), OUTFIT_ID);

    assertThat(spec.model()).isEqualTo("lucy-vton-latest");
    assertThat(spec.prompt()).isNotBlank();
    assertThat(spec.referenceImageUrl()).isNotBlank();
  }

  @Test
  void updatesSelectedOutfitIdOnSession() {
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    DecartSpecService service = serviceWith(session);

    service.getOutfitSpec(session.getSessionId(), OUTFIT_ID);

    assertThat(session.getSelectedOutfitId()).isEqualTo(OUTFIT_ID);
  }

  @Test
  void getOutfitSpecThrowsSessionNotFound() {
    DecartSpecService service = serviceWith(new InMemorySessionCache());

    assertThatThrownBy(() -> service.getOutfitSpec("unknown-session-id", OUTFIT_ID))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
  }

  @Test
  void getOutfitSpecThrowsInvalidSessionStateWhenNotTransformationReady() {
    ClientSession session = sessionWith(SessionStatus.BRIEFING);
    DecartSpecService service = serviceWith(session);

    assertThatThrownBy(() -> service.getOutfitSpec(session.getSessionId(), OUTFIT_ID))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_SESSION_STATE);
  }

  @Test
  void getOutfitSpecThrowsNotFoundForUnknownOutfitId() {
    ClientSession session = sessionWith(SessionStatus.TRANSFORMATION_READY);
    DecartSpecService service = serviceWith(session);

    assertThatThrownBy(() -> service.getOutfitSpec(session.getSessionId(), "unknown_outfit"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  private DecartSpecService serviceWith(ClientSession session) {
    return serviceWith(new InMemorySessionCache(session));
  }

  private DecartSpecService serviceWith(InMemorySessionCache sessionCache) {
    return new DecartSpecService(
        sessionCache, new SessionReader(sessionCache), () -> CLIENT_TOKEN, resolverWithOutfit());
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
    return TestClientSessions.sessionWith(status);
  }
}
