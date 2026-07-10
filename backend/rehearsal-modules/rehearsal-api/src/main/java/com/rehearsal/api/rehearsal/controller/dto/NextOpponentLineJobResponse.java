package com.rehearsal.api.rehearsal.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.OpponentLineJob;
import com.rehearsal.domain.rehearsal.model.OpponentLineJobStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Description("next opponent line job 상태 응답. status에 따라 담기는 필드가 다르다")
public record NextOpponentLineJobResponse(
    String sessionId,
    int turnNo,
    OpponentLineJobStatus status,
    String opponentLine,
    Boolean fallback,
    String message) {

  private static final String FAILURE_MESSAGE = "다시 시도해보세요.";

  public static NextOpponentLineJobResponse from(OpponentLineJob job) {
    return switch (job.status()) {
      case PENDING ->
          new NextOpponentLineJobResponse(
              job.sessionId(), job.turnNo(), job.status(), null, null, null);
      case COMPLETED ->
          new NextOpponentLineJobResponse(
              job.sessionId(),
              job.turnNo(),
              job.status(),
              job.result().opponentLine(),
              job.result().fallback(),
              null);
      case FAILED ->
          new NextOpponentLineJobResponse(
              job.sessionId(), job.turnNo(), job.status(), null, null, FAILURE_MESSAGE);
    };
  }
}
