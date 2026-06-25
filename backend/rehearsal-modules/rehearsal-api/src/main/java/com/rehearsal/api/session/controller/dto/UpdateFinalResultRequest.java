package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.session.usecase.command.UpdateFinalResultCommand;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateFinalResultRequest(@NotNull Map<String, Object> finalResult) {

  public UpdateFinalResultCommand toCommand(String sessionId) {
    return new UpdateFinalResultCommand(sessionId, finalResult);
  }
}
