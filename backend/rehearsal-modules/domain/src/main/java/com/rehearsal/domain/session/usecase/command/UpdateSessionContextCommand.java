package com.rehearsal.domain.session.usecase.command;

import java.util.List;
import java.util.Map;

public record UpdateSessionContextCommand(
    String sessionId,
    Map<String, Object> partialContext,
    List<String> missingRequiredSlotKeys,
    String followUpQuestion) {}
