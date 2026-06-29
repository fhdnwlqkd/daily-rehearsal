package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlotSchema;
import java.util.List;

public record UpdateContextSlotSchemaCommand(
    Long schemaId, String name, int maxFollowUpAttempt, boolean active) {

  public ContextSlotSchema toDomain() {
    return new ContextSlotSchema(schemaId, null, name, maxFollowUpAttempt, active, List.of());
  }
}
