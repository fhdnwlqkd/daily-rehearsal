package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.session.usecase.command.UpdateFeedbackResultCommand;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateFeedbackResultRequest(@NotNull Map<String, Object> feedbackResult) {

  public UpdateFeedbackResultCommand toCommand(String sessionId) {
    return new UpdateFeedbackResultCommand(sessionId, feedbackResult);
  }
}
