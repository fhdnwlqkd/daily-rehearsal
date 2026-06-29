package com.rehearsal.api.slot.controller.dto;

import com.rehearsal.domain.slot.usecase.command.CreateContextSlotSchemaCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateSchemaRequest(
    @NotBlank String schemaKey,
    @NotBlank String name,
    @PositiveOrZero int maxFollowUpAttempt,
    boolean active) {

  public CreateContextSlotSchemaCommand toCommand() {
    return new CreateContextSlotSchemaCommand(schemaKey, name, maxFollowUpAttempt, active);
  }
}
