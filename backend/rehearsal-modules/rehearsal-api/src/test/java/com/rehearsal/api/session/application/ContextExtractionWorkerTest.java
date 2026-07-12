package com.rehearsal.api.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.rehearsal.api.slot.application.ContextSlotExtractionService;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.api.support.InMemorySessionRepository;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextExtractionWorkerTest {

  @Test
  void briefingCompletionPersistsContextAndSessionState() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    ContextSlotExtractionService extractionService = mock(ContextSlotExtractionService.class);
    given(extractionService.extract(any()))
        .willReturn(result(Map.of("desired_persona", "warm_natural"), List.of(), true));

    worker(repository, extractionService).extractBriefing(session.getSessionId(), "transcript");

    assertThat(repository.findSession(session.getSessionId()).orElseThrow().getContextStatus())
        .isEqualTo(ContextStatus.COMPLETED);
    assertThat(repository.findContext(session.getSessionId()).orElseThrow().values())
        .containsEntry("desired_persona", "warm_natural");
  }

  @Test
  void followUpResultCanRequireAnotherRound() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    session.requireFollowUp();
    session.startFollowUpMerge();
    InMemorySessionRepository repository = new InMemorySessionRepository(session);
    repository.saveContext(
        session.getSessionId(),
        SessionContext.from(SituationType.DATE, Map.of("desired_persona", "warm_natural")));
    ContextSlotExtractionService extractionService = mock(ContextSlotExtractionService.class);
    given(extractionService.extract(any()))
        .willReturn(
            result(
                Map.of("desired_persona", "warm_natural"),
                List.of("critical_moment"),
                false));

    worker(repository, extractionService).extractFollowUp(session.getSessionId(), "transcript");

    assertThat(repository.findSession(session.getSessionId()).orElseThrow().getContextStatus())
        .isEqualTo(ContextStatus.FOLLOW_UP_REQUIRED);
  }

  private ContextExtractionWorker worker(
      InMemorySessionRepository repository, ContextSlotExtractionService extractionService) {
    return new ContextExtractionWorker(
        new SessionReader(repository), repository, extractionService);
  }

  private ExtractContextSlotsResult result(
      Map<String, Object> context, List<String> missingSlotKeys, boolean ready) {
    return new ExtractContextSlotsResult(
        "date",
        Map.of(),
        Map.<String, ContextSlotValue>of(),
        context,
        missingSlotKeys,
        missingSlotKeys.isEmpty() ? null : "follow-up question",
        ready);
  }
}
