package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.SlotType;
import java.util.List;

public record CreateContextSlotCommand(
    String slotKey,
    String label,
    SlotType slotType,
    String extractionHint,
    String followUpHint,
    String defaultLiteralValue,
    Long defaultOptionId) {

  public ContextSlot toDomain() {
    return new ContextSlot(
        null,
        slotKey,
        label,
        slotType,
        extractionHint,
        followUpHint,
        defaultLiteralValue,
        null,
        List.of());
  }
}
