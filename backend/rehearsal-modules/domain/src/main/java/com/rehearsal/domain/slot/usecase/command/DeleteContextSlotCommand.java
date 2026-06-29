package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlot;
import java.util.List;

public record DeleteContextSlotCommand(Long slotId) {

  public ContextSlot toDomain() {
    return new ContextSlot(slotId, null, null, null, null, null, null, null, List.of());
  }
}
