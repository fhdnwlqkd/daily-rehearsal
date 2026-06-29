package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import java.util.List;

public record UpdateContextSlotCommand(
    Long slotId,
    String label,
    String extractionHint,
    String followUpHint,
    String defaultLiteralValue,
    Long defaultOptionId) {

  public ContextSlot toDomain() {
    ContextSlotOption defaultOption =
        defaultOptionId == null ? null : new ContextSlotOption(defaultOptionId, null, null);
    return new ContextSlot(
        slotId,
        null,
        label,
        null,
        extractionHint,
        followUpHint,
        defaultLiteralValue,
        defaultOption,
        List.of());
  }
}
