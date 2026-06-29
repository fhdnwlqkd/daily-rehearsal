package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlotSchema;
import java.util.List;

public record DeleteContextSlotSchemaCommand(Long schemaId) {

  public ContextSlotSchema toDomain() {
    return new ContextSlotSchema(schemaId, null, null, 0, false, List.of());
  }
}
