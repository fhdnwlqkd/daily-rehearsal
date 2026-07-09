package com.rehearsal.domain.extraction.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextExtractionJobTest {

  @Test
  void pendingCreatesJobIdAndPendingStatus() {
    ContextExtractionJob job =
        ContextExtractionJob.pending(
            "session-id", SituationType.DATE, ContextExtractionJobType.BRIEFING);

    assertThat(job.sessionId()).isEqualTo("session-id");
    assertThat(job.jobId()).isNotBlank();
    assertThat(job.situationType()).isEqualTo(SituationType.DATE);
    assertThat(job.type()).isEqualTo(ContextExtractionJobType.BRIEFING);
    assertThat(job.status()).isEqualTo(ContextExtractionJobStatus.PENDING);
  }

  @Test
  void completeWithFinalContextStoresFinalContext() {
    ContextExtractionJob job =
        ContextExtractionJob.pending(
            "session-id", SituationType.DATE, ContextExtractionJobType.BRIEFING);
    SessionContext context =
        SessionContext.from(SituationType.DATE, Map.of("desired_persona", "warm_natural"));

    ContextExtractionJob completed = job.completeWithFinalContext(context);

    assertThat(completed.status()).isEqualTo(ContextExtractionJobStatus.COMPLETED);
    assertThat(completed.finalContext().valuesWithSituationType())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("situation_type", "date");
    assertThat(completed.followUpQuestions()).isEmpty();
  }

  @Test
  void completeWithFollowUpQuestionsStoresQuestions() {
    ContextExtractionJob job =
        ContextExtractionJob.pending(
            "session-id", SituationType.DATE, ContextExtractionJobType.FOLLOW_UP);

    ContextExtractionJob completed =
        job.completeWithFollowUpQuestions(List.of("Which moment should we focus on?"));

    assertThat(completed.status()).isEqualTo(ContextExtractionJobStatus.COMPLETED);
    assertThat(completed.finalContext()).isNull();
    assertThat(completed.followUpQuestions()).containsExactly("Which moment should we focus on?");
  }
}
