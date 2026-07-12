package com.rehearsal.api.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rehearsal.api.slot.application.ContextSlotExtractionService;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.api.support.InMemoryContextExtractionJobStore;
import com.rehearsal.api.support.InMemorySessionCache;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.model.ContextExtractionJobStatus;
import com.rehearsal.domain.extraction.model.ContextExtractionJobType;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.port.ContextExtractionJobStore;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextExtractionWorkerTest {

  @Test
  void extractBriefingAsyncCompletesContextAndJob() {
    ClientSession session = ClientSession.create(SituationType.DATE);
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    ContextExtractionJobStore jobStore = new InMemoryContextExtractionJobStore();
    ContextSlotExtractionService extractionService = mock(ContextSlotExtractionService.class);
    ContextExtractionWorker worker = worker(sessionCache, jobStore, extractionService);
    ContextExtractionJob job =
        ContextExtractionJob.pending(
            session.getSessionId(), session.getSituationType(), ContextExtractionJobType.BRIEFING);
    jobStore.save(job);

    when(extractionService.extract(any()))
        .thenReturn(result(Map.of("desired_persona", "warm_natural"), List.of(), null, true));

    worker.extractBriefingAsync(job, "briefing transcript");

    ClientSession updated = sessionCache.findById(session.getSessionId()).orElseThrow();
    ContextExtractionJob completed =
        jobStore.findById(session.getSessionId(), job.jobId()).orElseThrow();

    assertThat(updated.getStatus()).isEqualTo(SessionStatus.TRANSFORMATION_READY);
    assertThat(updated.getContextStatus()).isEqualTo(ContextStatus.COMPLETED);
    assertThat(updated.getFinalContext().valuesWithSituationType())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("situation_type", "date");
    assertThat(completed.status()).isEqualTo(ContextExtractionJobStatus.COMPLETED);
    assertThat(completed.finalContext().valuesWithSituationType())
        .containsEntry("desired_persona", "warm_natural");
  }

  @Test
  void extractFollowUpAsyncMergesContextAndCompletesJob() {
    ClientSession session = followUpRequiredSession();
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    ContextExtractionJobStore jobStore = new InMemoryContextExtractionJobStore();
    ContextSlotExtractionService extractionService = mock(ContextSlotExtractionService.class);
    ContextExtractionWorker worker = worker(sessionCache, jobStore, extractionService);
    ContextExtractionJob job =
        ContextExtractionJob.pending(
            session.getSessionId(), session.getSituationType(), ContextExtractionJobType.FOLLOW_UP);
    jobStore.save(job);

    when(extractionService.extract(any()))
        .thenReturn(result(Map.of("critical_moment", "first greeting"), List.of(), null, true));

    worker.extractFollowUpAsync(job, "follow-up transcript");

    ClientSession updated = sessionCache.findById(session.getSessionId()).orElseThrow();
    ContextExtractionJob completed =
        jobStore.findById(session.getSessionId(), job.jobId()).orElseThrow();

    assertThat(updated.getStatus()).isEqualTo(SessionStatus.TRANSFORMATION_READY);
    assertThat(updated.getFollowUpAttempt()).isEqualTo(1);
    assertThat(updated.getFinalContext().valuesWithSituationType())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("critical_moment", "first greeting")
        .containsEntry("situation_type", "date");
    assertThat(completed.status()).isEqualTo(ContextExtractionJobStatus.COMPLETED);
    assertThat(completed.finalContext().valuesWithSituationType())
        .containsEntry("critical_moment", "first greeting");
  }

  private ContextExtractionWorker worker(
      InMemorySessionCache sessionCache,
      ContextExtractionJobStore jobStore,
      ContextSlotExtractionService extractionService) {
    return new ContextExtractionWorker(
        new SessionReader(sessionCache), sessionCache, extractionService, jobStore);
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
        SessionContext.from(SituationType.DATE, Map.of("desired_persona", "warm_natural")),
        List.of("critical_moment"),
        List.of("Which moment are you most worried about?"));
    return session;
  }
}
