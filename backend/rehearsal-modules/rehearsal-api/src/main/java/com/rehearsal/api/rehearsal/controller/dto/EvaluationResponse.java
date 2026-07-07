package com.rehearsal.api.rehearsal.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;

@Description("turn 성공/실패와 피드백만 담는 evaluation 응답")
public record EvaluationResponse(boolean success, String feedback) {

  public static EvaluationResponse from(TurnEvaluationResult result) {
    return new EvaluationResponse(result.success(), result.feedback());
  }
}
