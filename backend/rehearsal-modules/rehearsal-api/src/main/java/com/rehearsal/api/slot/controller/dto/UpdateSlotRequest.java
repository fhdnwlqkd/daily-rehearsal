package com.rehearsal.api.slot.controller.dto;

import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotCommand;
import jakarta.validation.constraints.NotBlank;

public record UpdateSlotRequest(
    @NotBlank String label,
    String extractionHint,
    String followUpHint,
    String defaultLiteralValue,
    Long defaultOptionId) {

  public UpdateContextSlotCommand toCommand(Long slotId) {
    return new UpdateContextSlotCommand(
        slotId, label, extractionHint, followUpHint, defaultLiteralValue, defaultOptionId);
  }
}
