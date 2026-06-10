package com.rehearsal.domain.slot.model;

import java.util.List;

public record ContextSlot(
    Long id,
    String slotKey,
    String label,
    SlotType slotType,
    String extractionHint,
    String followUpHint,
    String defaultLiteralValue,
    ContextSlotOption defaultOption,
    List<ContextSlotOption> options) {

  public ContextSlot {
    options = options == null ? List.of() : List.copyOf(options);
  }
}
