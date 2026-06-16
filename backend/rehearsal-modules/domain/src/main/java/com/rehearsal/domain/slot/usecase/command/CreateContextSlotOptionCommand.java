package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import java.util.List;

public record CreateContextSlotOptionCommand(Long slotId, String optionKey, String label) {

  public ContextSlot toDomain() {
    return new ContextSlot(
        slotId,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(new ContextSlotOption(null, optionKey, label)));
  }
}
