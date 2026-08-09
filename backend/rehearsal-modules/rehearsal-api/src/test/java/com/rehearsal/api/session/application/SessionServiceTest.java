package com.rehearsal.api.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.rehearsal.api.decart.application.OutfitSpecResolver;
import com.rehearsal.api.support.InMemorySessionRepository;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextCollectionState;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SessionServiceTest {

  @Test
  void submitBriefingPersistsExtractingStateAndPublishesRequest() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    List<Object> events = new ArrayList<>();
    SessionService service = service(repository, events);

    ClientSession submitted =
        service.submitBriefingExtraction(session.getSessionId(), "briefing transcript");

    assertThat(submitted.getContextStatus()).isEqualTo(ContextStatus.EXTRACTING);
    assertThat(repository.findSession(session.getSessionId())).contains(submitted);
    assertThat(events)
        .containsExactly(
            new ContextExtractionRequested(
                session.getSessionId(), "briefing transcript", SlotExtractionMode.INITIAL));
  }

  @Test
  void submitFollowUpPersistsMergingStateAndIncreasesAttempt() {
    ClientSession session = followUpRequiredSession();
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    List<Object> events = new ArrayList<>();
    SessionService service = service(repository, events);

    ClientSession submitted =
        service.submitFollowUpExtraction(session.getSessionId(), "follow-up transcript");

    assertThat(submitted.getContextStatus()).isEqualTo(ContextStatus.MERGING);
    assertThat(submitted.getFollowUpAttempt()).isEqualTo(1);
    assertThat(events)
        .containsExactly(
            new ContextExtractionRequested(
                session.getSessionId(), "follow-up transcript", SlotExtractionMode.FOLLOW_UP));
  }

  @Test
  void getContextReadsSessionStatusAndPersistedValues() {
    ClientSession session = followUpRequiredSession();
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    repository.saveContext(
        session.getSessionId(),
        SessionContext.from(SituationType.DATE, Map.of("desired_persona", "warm_natural")));

    ContextCollectionState state =
        service(repository, new ArrayList<>()).getContext(session.getSessionId());

    assertThat(state.status()).isEqualTo(ContextStatus.FOLLOW_UP_REQUIRED);
    assertThat(state.context().values()).containsEntry("desired_persona", "warm_natural");
    assertThat(state.missingSlotKeys()).contains("critical_moment");
    assertThat(state.followUpQuestions()).isNotEmpty();
  }

  private SessionService service(InMemorySessionRepository repository, List<Object> events) {
    return new SessionService(
        repository, new SessionReader(repository), mock(OutfitSpecResolver.class), events::add);
  }

  private ClientSession followUpRequiredSession() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    session.requireFollowUp();
    return session;
  }
}
