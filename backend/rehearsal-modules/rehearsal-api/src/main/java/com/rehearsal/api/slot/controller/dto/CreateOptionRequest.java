package com.rehearsal.api.slot.controller.dto;

import com.rehearsal.domain.slot.usecase.command.CreateContextSlotOptionCommand;
import jakarta.validation.constraints.NotBlank;

public record CreateOptionRequest(@NotBlank String optionKey, @NotBlank String label) {

  public CreateContextSlotOptionCommand toCommand(Long slotId) {
    return new CreateContextSlotOptionCommand(slotId, optionKey, label);
  }
}
