package com.rehearsal.api.session.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.model.ContextExtractionJobStatus;
import com.rehearsal.domain.extraction.model.ContextExtractionJobType;
import com.rehearsal.domain.session.model.SessionContext;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Description("Context extraction job status response. Fields vary by status.")
public record ContextExtractionJobResponse(
    String sessionId,
    String jobId,
    ContextExtractionJobType type,
    ContextExtractionJobStatus status,
    Map<String, Object> finalContext,
    List<String> followUpQuestions,
    String message) {

  private static final String FAILURE_MESSAGE = "Please try again.";

  public static ContextExtractionJobResponse from(ContextExtractionJob job) {
    return switch (job.status()) {
      case PENDING ->
          new ContextExtractionJobResponse(
              job.sessionId(), job.jobId(), job.type(), job.status(), null, null, null);
      case COMPLETED ->
          new ContextExtractionJobResponse(
              job.sessionId(),
              job.jobId(),
              job.type(),
              job.status(),
              contextValues(job.finalContext()),
              job.followUpQuestions(),
              null);
      case FAILED ->
          new ContextExtractionJobResponse(
              job.sessionId(), job.jobId(), job.type(), job.status(), null, null, FAILURE_MESSAGE);
    };
  }

  private static Map<String, Object> contextValues(SessionContext context) {
    return context == null ? null : context.valuesWithSituationType();
  }
}
