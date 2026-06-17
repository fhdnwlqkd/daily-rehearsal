package com.rehearsal.api.slot.controller.dto;

import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotSchemaItemCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateSchemaItemRequest(
    @NotNull Long slotId,
    @NotNull RequiredLevel requiredLevel,
    @PositiveOrZero int priority,
    boolean active) {

  public CreateContextSlotSchemaItemCommand toCommand(Long schemaId) {
    return new CreateContextSlotSchemaItemCommand(
        schemaId, slotId, requiredLevel, priority, active);
  }
}
