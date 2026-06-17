package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;

public record UpdateContextSlotSchemaItemCommand(
    Long itemId, RequiredLevel requiredLevel, int priority, boolean active) {

  public ContextSlotSchemaItem toDomain() {
    return new ContextSlotSchemaItem(itemId, null, requiredLevel, priority, active);
  }
}
