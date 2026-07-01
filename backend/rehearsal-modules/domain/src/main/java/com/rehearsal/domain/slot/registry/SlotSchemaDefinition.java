package com.rehearsal.domain.slot.registry;

import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;

public record SlotSchemaDefinition(
    SituationType situationType,
    String name,
    int maxFollowUpAttempt,
    List<SlotSchemaItemDefinition> items) {

  public SlotSchemaDefinition {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
