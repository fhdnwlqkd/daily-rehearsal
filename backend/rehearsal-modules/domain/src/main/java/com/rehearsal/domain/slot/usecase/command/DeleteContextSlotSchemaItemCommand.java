package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;

public record DeleteContextSlotSchemaItemCommand(Long itemId) {

  public ContextSlotSchemaItem toDomain() {
    return new ContextSlotSchemaItem(itemId, null, null, 0, false);
  }
}
