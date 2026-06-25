package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.session.usecase.command.UpdateSessionContextCommand;
import java.util.List;
import java.util.Map;

public record UpdateSessionContextRequest(
    Map<String, Object> partialContext,
    List<String> missingRequiredSlotKeys,
    String followUpQuestion) {

  public UpdateSessionContextCommand toCommand(String sessionId) {
    return new UpdateSessionContextCommand(
        sessionId, partialContext, missingRequiredSlotKeys, followUpQuestion);
  }
}
