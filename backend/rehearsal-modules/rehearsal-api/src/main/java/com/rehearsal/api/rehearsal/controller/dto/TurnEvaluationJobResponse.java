package com.rehearsal.api.rehearsal.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJobStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Description("turn evaluation job 상태 응답. status에 따라 담기는 필드가 다르다")
public record TurnEvaluationJobResponse(
    String sessionId,
    int turnNo,
    TurnEvaluationJobStatus status,
    Boolean success,
    String feedback,
    Boolean fallback,
    String message) {

  private static final String FAILURE_MESSAGE = "다시 시도해보세요.";

  public static TurnEvaluationJobResponse from(TurnEvaluationJob job) {
    return switch (job.status()) {
      case PENDING ->
          new TurnEvaluationJobResponse(
              job.sessionId(), job.turnNo(), job.status(), null, null, null, null);
      case COMPLETED ->
          new TurnEvaluationJobResponse(
              job.sessionId(),
              job.turnNo(),
              job.status(),
              job.result().success(),
              job.result().feedback(),
              job.result().fallback(),
              null);
      case FAILED ->
          new TurnEvaluationJobResponse(
              job.sessionId(), job.turnNo(), job.status(), null, null, null, FAILURE_MESSAGE);
    };
  }
}
