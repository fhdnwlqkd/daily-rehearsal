package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.session.usecase.command.CompleteSessionContextCommand;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CompleteSessionContextRequest(@NotNull Map<String, Object> finalUserContext) {

  public CompleteSessionContextCommand toCommand(String sessionId) {
    return new CompleteSessionContextCommand(sessionId, finalUserContext);
  }
}
