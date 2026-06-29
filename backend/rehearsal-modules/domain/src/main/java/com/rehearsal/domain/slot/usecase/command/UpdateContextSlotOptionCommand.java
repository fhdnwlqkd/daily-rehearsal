package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlotOption;

public record UpdateContextSlotOptionCommand(Long optionId, String label) {

  public ContextSlotOption toDomain() {
    return new ContextSlotOption(optionId, null, label);
  }
}
