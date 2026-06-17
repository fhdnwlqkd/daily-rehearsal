package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlotSchema;
import java.util.List;

public record CreateContextSlotSchemaCommand(
    String schemaKey, String name, int maxFollowUpAttempt, boolean active) {

  public ContextSlotSchema toDomain() {
    return new ContextSlotSchema(null, schemaKey, name, maxFollowUpAttempt, active, List.of());
  }
}
