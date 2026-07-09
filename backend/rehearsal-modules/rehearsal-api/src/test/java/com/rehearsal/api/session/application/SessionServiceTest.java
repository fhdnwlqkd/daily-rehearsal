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
import com.rehearsal.api.support.InMemoryContextExtractionJobStore;
import com.rehearsal.api.support.InMemorySessionCache;
import com.rehearsal.api.support.RecordingContextExtractionWorker;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.model.ContextExtractionJobStatus;
import com.rehearsal.domain.extraction.model.ContextExtractionJobType;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.extraction.port.ContextExtractionJobStore;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
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
    assertThat(updated.getFinalContext().valuesWithSituationType())
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
    assertThat(updated.getPartialContext().valuesWithSituationType())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("situation_type", "date");
    assertThat(updated.getMissingSlotKeys()).containsExactly("critical_moment");
    assertThat(updated.getFollowUpQuestions())
        .containsExactly("Which moment are you most worried about?");
    assertThat(updated.getFinalContext()).isNull();
  }

  @Test
  void submitFollowUpMergesExtractedContextIntoPartialContextWhenContextIsComplete() {
    ClientSession session = followUpRequiredSession();
    SessionCache sessionCache = new InMemorySessionCache(session);
    ContextSlotExtractionService extractionService = mock(ContextSlotExtractionService.class);
    SessionService service = service(sessionCache, extractionService);

    when(extractionService.extract(any()))
        .thenReturn(result(Map.of("critical_moment", "first greeting"), List.of(), null, true));

    ClientSession updated = service.submitFollowUp(session.getSessionId(), "follow-up transcript");

    assertThat(updated.getStatus()).isEqualTo(SessionStatus.TRANSFORMATION_READY);
    assertThat(updated.getContextStatus()).isEqualTo(ContextStatus.COMPLETED);
    assertThat(updated.getFollowUpAttempt()).isEqualTo(1);
    assertThat(updated.getFinalContext().valuesWithSituationType())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("critical_moment", "first greeting")
        .containsEntry("situation_type", "date");
    assertThat(updated.getMissingSlotKeys()).isEmpty();
    assertThat(updated.getFollowUpQuestions()).isEmpty();

    ArgumentCaptor<ExtractContextSlotsCommand> commandCaptor =
        ArgumentCaptor.forClass(ExtractContextSlotsCommand.class);
    verify(extractionService).extract(commandCaptor.capture());
    assertThat(commandCaptor.getValue().mode()).isEqualTo(SlotExtractionMode.FOLLOW_UP);
    assertThat(commandCaptor.getValue().followUpAttempt()).isEqualTo(1);
    assertThat(commandCaptor.getValue().currentSlots())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("situation_type", "date");
    assertThat(commandCaptor.getValue().targetSlotKeys()).containsExactly("critical_moment");
  }

  @Test
  void submitFollowUpKeepsPartialContextWhenAnotherFollowUpIsRequired() {
    ClientSession session = followUpRequiredSession();
    SessionCache sessionCache = new InMemorySessionCache(session);
    ContextSlotExtractionService extractionService = mock(ContextSlotExtractionService.class);
    SessionService service = service(sessionCache, extractionService);

    when(extractionService.extract(any()))
        .thenReturn(
            result(
                Map.of("critical_moment", "first greeting"),
                List.of("relationship_context"),
                "What is your relationship with them?",
                false));

    ClientSession updated = service.submitFollowUp(session.getSessionId(), "follow-up transcript");

    assertThat(updated.getStatus()).isEqualTo(SessionStatus.FOLLOW_UP_REQUIRED);
    assertThat(updated.getContextStatus()).isEqualTo(ContextStatus.FOLLOW_UP_REQUIRED);
    assertThat(updated.getFollowUpAttempt()).isEqualTo(1);
    assertThat(updated.getPartialContext().valuesWithSituationType())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("critical_moment", "first greeting")
        .containsEntry("situation_type", "date");
    assertThat(updated.getMissingSlotKeys()).containsExactly("relationship_context");
    assertThat(updated.getFollowUpQuestions())
        .containsExactly("What is your relationship with them?");
    assertThat(updated.getFinalContext()).isNull();
  }

  @Test
  void submitBriefingExtractionCreatesPendingJobAndDispatchesWorker() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    SessionCache sessionCache = new InMemorySessionCache(session);
    ContextExtractionJobStore jobStore = new InMemoryContextExtractionJobStore();
    RecordingContextExtractionWorker worker = new RecordingContextExtractionWorker();
    SessionService service =
        service(sessionCache, mock(ContextSlotExtractionService.class), jobStore, worker);

    ContextExtractionJob job =
        service.submitBriefingExtraction(session.getSessionId(), "briefing transcript");

    assertThat(job.status()).isEqualTo(ContextExtractionJobStatus.PENDING);
    assertThat(job.type()).isEqualTo(ContextExtractionJobType.BRIEFING);
    assertThat(jobStore.findById(session.getSessionId(), job.jobId())).contains(job);
    assertThat(worker.briefingInvocationCount()).isEqualTo(1);
  }

  @Test
  void submitFollowUpExtractionCreatesPendingJobAndDispatchesWorker() {
    ClientSession session = followUpRequiredSession();
    SessionCache sessionCache = new InMemorySessionCache(session);
    ContextExtractionJobStore jobStore = new InMemoryContextExtractionJobStore();
    RecordingContextExtractionWorker worker = new RecordingContextExtractionWorker();
    SessionService service =
        service(sessionCache, mock(ContextSlotExtractionService.class), jobStore, worker);

    ContextExtractionJob job =
        service.submitFollowUpExtraction(session.getSessionId(), "follow-up transcript");

    assertThat(job.status()).isEqualTo(ContextExtractionJobStatus.PENDING);
    assertThat(job.type()).isEqualTo(ContextExtractionJobType.FOLLOW_UP);
    assertThat(jobStore.findById(session.getSessionId(), job.jobId())).contains(job);
    assertThat(worker.followUpInvocationCount()).isEqualTo(1);
  }

  private SessionService service(
      SessionCache sessionCache, ContextSlotExtractionService extractionService) {
    return service(
        sessionCache,
        extractionService,
        new InMemoryContextExtractionJobStore(),
        new RecordingContextExtractionWorker());
  }

  private SessionService service(
      SessionCache sessionCache,
      ContextSlotExtractionService extractionService,
      ContextExtractionJobStore jobStore,
      ContextExtractionWorker worker) {
    return new SessionService(
        sessionCache,
        new SessionReader(sessionCache),
        mock(OutfitSpecResolver.class),
        extractionService,
        jobStore,
        worker);
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

  private ClientSession followUpRequiredSession() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    session.requireFollowUp(
        SessionContext.from(
            SituationType.DATE,
            Map.of("situation_type", "date", "desired_persona", "warm_natural")),
        List.of("critical_moment"),
        List.of("Which moment are you most worried about?"));
    return session;
  }
}
