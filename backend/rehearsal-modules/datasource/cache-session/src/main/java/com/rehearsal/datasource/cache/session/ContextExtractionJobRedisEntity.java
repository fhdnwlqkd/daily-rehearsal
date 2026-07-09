package com.rehearsal.datasource.cache.session;

import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.model.ContextExtractionJobStatus;
import com.rehearsal.domain.extraction.model.ContextExtractionJobType;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;

public record ContextExtractionJobRedisEntity(
    String sessionId,
    String jobId,
    String situationType,
    ContextExtractionJobType type,
    ContextExtractionJobStatus status,
    Map<String, Object> finalContext,
    List<String> followUpQuestions,
    String failureReason) {

  public static ContextExtractionJobRedisEntity from(ContextExtractionJob job) {
    return new ContextExtractionJobRedisEntity(
        job.sessionId(),
        job.jobId(),
        job.situationType().key(),
        job.type(),
        job.status(),
        contextValues(job.finalContext()),
        job.followUpQuestions(),
        job.failureReason());
  }

  public ContextExtractionJob toDomain() {
    SituationType restoredSituationType = restoreSituationType();
    return new ContextExtractionJob(
        sessionId,
        jobId,
        restoredSituationType,
        type,
        status,
        restoreContext(restoredSituationType, finalContext),
        followUpQuestions,
        failureReason);
  }

  private SituationType restoreSituationType() {
    if (situationType == null || situationType.isBlank()) {
      return SituationType.DATE;
    }
    return SituationType.fromKey(situationType);
  }

  private static Map<String, Object> contextValues(SessionContext context) {
    return context == null ? null : context.valuesWithSituationType();
  }

  private static SessionContext restoreContext(
      SituationType situationType, Map<String, Object> context) {
    return context == null ? null : SessionContext.from(situationType, context);
  }
}
