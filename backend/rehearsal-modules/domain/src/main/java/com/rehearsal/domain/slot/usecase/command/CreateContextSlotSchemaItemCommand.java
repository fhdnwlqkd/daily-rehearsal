package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
import java.util.List;

public record CreateContextSlotSchemaItemCommand(
    Long schemaId, Long slotId, RequiredLevel requiredLevel, int priority, boolean active) {

  public ContextSlotSchema toDomain() {
    ContextSlot slot = new ContextSlot(slotId, null, null, null, null, null, null, null, List.of());
    ContextSlotSchemaItem item =
        new ContextSlotSchemaItem(null, slot, requiredLevel, priority, active);
    return new ContextSlotSchema(schemaId, null, null, 0, true, List.of(item));
  }
}
