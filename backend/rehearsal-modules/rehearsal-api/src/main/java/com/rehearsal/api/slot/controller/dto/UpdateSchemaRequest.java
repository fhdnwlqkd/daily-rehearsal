package com.rehearsal.api.slot.controller.dto;

import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotSchemaCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateSchemaRequest(
    @NotBlank String name, @PositiveOrZero int maxFollowUpAttempt, boolean active) {

  public UpdateContextSlotSchemaCommand toCommand(Long schemaId) {
    return new UpdateContextSlotSchemaCommand(schemaId, name, maxFollowUpAttempt, active);
  }
}
