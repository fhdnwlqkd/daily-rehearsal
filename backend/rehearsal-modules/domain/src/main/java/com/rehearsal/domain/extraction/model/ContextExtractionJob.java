package com.rehearsal.domain.extraction.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.UUID;

@Description("Briefing/follow-up context extraction async job state")
public record ContextExtractionJob(
    String sessionId,
    String jobId,
    SituationType situationType,
    ContextExtractionJobType type,
    ContextExtractionJobStatus status,
    @Description("COMPLETED and context is complete when non-null") SessionContext finalContext,
    @Description("COMPLETED and follow-up is required when non-empty")
        List<String> followUpQuestions,
    @Description("Server log/debug only. Do not expose directly to clients.")
        String failureReason) {

  public ContextExtractionJob {
    followUpQuestions = followUpQuestions == null ? List.of() : List.copyOf(followUpQuestions);
  }

  public static ContextExtractionJob pending(
      String sessionId, SituationType situationType, ContextExtractionJobType type) {
    return new ContextExtractionJob(
        sessionId,
        UUID.randomUUID().toString(),
        situationType,
        type,
        ContextExtractionJobStatus.PENDING,
        null,
        List.of(),
        null);
  }

  public ContextExtractionJob completeWithFinalContext(SessionContext finalContext) {
    return new ContextExtractionJob(
        sessionId,
        jobId,
        situationType,
        type,
        ContextExtractionJobStatus.COMPLETED,
        finalContext,
        List.of(),
        null);
  }

  public ContextExtractionJob completeWithFollowUpQuestions(List<String> followUpQuestions) {
    return new ContextExtractionJob(
        sessionId,
        jobId,
        situationType,
        type,
        ContextExtractionJobStatus.COMPLETED,
        null,
        followUpQuestions,
        null);
  }

  public ContextExtractionJob fail(String failureReason) {
    return new ContextExtractionJob(
        sessionId,
        jobId,
        situationType,
        type,
        ContextExtractionJobStatus.FAILED,
        null,
        List.of(),
        failureReason);
  }
}
