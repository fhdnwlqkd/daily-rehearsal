package com.rehearsal.api.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rehearsal.api.decart.application.OutfitSpecResolver;
import com.rehearsal.api.slot.application.ContextSlotExtractionService;
import com.rehearsal.api.slot.application.command.ExtractContextSlotsCommand;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.api.support.InMemorySessionCache;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SessionServiceTest {

  @Test
  void submitBriefingStoresFinalContextWhenContextIsComplete() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    SessionCache sessionCache = new InMemorySessionCache(session);
    ContextSlotExtractionService extractionService = mock(ContextSlotExtractionService.class);
    SessionService service = service(sessionCache, extractionService);

    when(extractionService.extract(any()))
        .thenReturn(result(Map.of("desired_persona", "warm_natural"), List.of(), null, true));

    ClientSession updated = service.submitBriefing(session.getSessionId(), "briefing transcript");

    assertThat(updated.getStatus()).isEqualTo(SessionStatus.TRANSFORMATION_READY);
    assertThat(updated.getContextStatus()).isEqualTo(ContextStatus.COMPLETED);
    assertThat(updated.getFinalContext())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("situation_type", "date");
    assertThat(updated.getFollowUpQuestions()).isEmpty();

    ArgumentCaptor<ExtractContextSlotsCommand> commandCaptor =
        ArgumentCaptor.forClass(ExtractContextSlotsCommand.class);
    verify(extractionService).extract(commandCaptor.capture());
    assertThat(commandCaptor.getValue().schemaKey()).isEqualTo("date");
    assertThat(commandCaptor.getValue().transcript()).isEqualTo("briefing transcript");
  }

  @Test
  void submitBriefingStoresPartialContextWhenFollowUpIsRequired() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    SessionCache sessionCache = new InMemorySessionCache(session);
    ContextSlotExtractionService extractionService = mock(ContextSlotExtractionService.class);
    SessionService service = service(sessionCache, extractionService);

    when(extractionService.extract(any()))
        .thenReturn(
            result(
                Map.of("desired_persona", "warm_natural"),
                List.of("critical_moment"),
                "Which moment are you most worried about?",
                false));

    ClientSession updated = service.submitBriefing(session.getSessionId(), "briefing transcript");

    assertThat(updated.getStatus()).isEqualTo(SessionStatus.FOLLOW_UP_REQUIRED);
    assertThat(updated.getContextStatus()).isEqualTo(ContextStatus.FOLLOW_UP_REQUIRED);
    assertThat(updated.getPartialContext())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("situation_type", "date");
    assertThat(updated.getMissingSlotKeys()).containsExactly("critical_moment");
    assertThat(updated.getFollowUpQuestions())
        .containsExactly("Which moment are you most worried about?");
    assertThat(updated.getFinalContext()).isNull();
  }

  private SessionService service(
      SessionCache sessionCache, ContextSlotExtractionService extractionService) {
    return new SessionService(
        sessionCache,
        new SessionReader(sessionCache),
        mock(OutfitSpecResolver.class),
        extractionService);
  }

  private ExtractContextSlotsResult result(
      Map<String, Object> context,
      List<String> missingSlotKeys,
      String followUpQuestion,
      boolean readyForSimulation) {
    return new ExtractContextSlotsResult(
        "date",
        Map.of(),
        Map.<String, ContextSlotValue>of(),
        context,
        missingSlotKeys,
        followUpQuestion,
        readyForSimulation);
  }
}
