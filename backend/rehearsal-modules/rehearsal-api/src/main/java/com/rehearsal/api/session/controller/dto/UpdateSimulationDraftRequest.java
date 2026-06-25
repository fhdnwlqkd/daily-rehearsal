package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.session.usecase.command.UpdateSimulationDraftCommand;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateSimulationDraftRequest(@NotNull Map<String, Object> simulationDraft) {

  public UpdateSimulationDraftCommand toCommand(String sessionId) {
    return new UpdateSimulationDraftCommand(sessionId, simulationDraft);
  }
}
