package com.rehearsal.api.slot.controller.dto;

import com.rehearsal.domain.slot.model.SlotType;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSlotRequest(
    @NotBlank String slotKey,
    @NotBlank String label,
    @NotNull SlotType slotType,
    String extractionHint,
    String followUpHint,
    String defaultLiteralValue,
    Long defaultOptionId) {

  public CreateContextSlotCommand toCommand() {
    return new CreateContextSlotCommand(
        slotKey,
        label,
        slotType,
        extractionHint,
        followUpHint,
        defaultLiteralValue,
        defaultOptionId);
  }
}
