package com.rehearsal.domain.slot.registry;

import com.rehearsal.domain.slot.model.SlotType;
import java.util.List;

public record SlotDefinition(
    String slotKey,
    String label,
    SlotType slotType,
    String extractionHint,
    String followUpHint,
    String defaultLiteralValue,
    String defaultOptionKey,
    List<SlotOptionDefinition> options) {

  public SlotDefinition {
    options = options == null ? List.of() : List.copyOf(options);
  }
}
