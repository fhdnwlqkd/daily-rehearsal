package com.rehearsal.domain.slot.model;

import java.util.List;

public record ContextSlotSchema(
    Long id,
    String schemaKey,
    String name,
    int maxFollowUpAttempt,
    boolean active,
    List<ContextSlotSchemaItem> items) {

  public ContextSlotSchema {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
