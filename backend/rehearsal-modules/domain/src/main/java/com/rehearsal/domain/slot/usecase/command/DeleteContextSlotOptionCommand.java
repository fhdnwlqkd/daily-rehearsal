package com.rehearsal.domain.slot.usecase.command;

import com.rehearsal.domain.slot.model.ContextSlotOption;

public record DeleteContextSlotOptionCommand(Long optionId) {

  public ContextSlotOption toDomain() {
    return new ContextSlotOption(optionId, null, null);
  }
}
