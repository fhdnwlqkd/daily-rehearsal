package com.rehearsal.api.slot.controller.dto;

import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotOptionCommand;
import jakarta.validation.constraints.NotBlank;

public record UpdateOptionRequest(@NotBlank String label) {

  public UpdateContextSlotOptionCommand toCommand(Long optionId) {
    return new UpdateContextSlotOptionCommand(optionId, label);
  }
}
