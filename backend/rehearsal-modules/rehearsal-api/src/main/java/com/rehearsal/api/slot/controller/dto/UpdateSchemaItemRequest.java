package com.rehearsal.api.slot.controller.dto;

import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotSchemaItemCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateSchemaItemRequest(
    @NotNull RequiredLevel requiredLevel, @PositiveOrZero int priority, boolean active) {

  public UpdateContextSlotSchemaItemCommand toCommand(Long itemId) {
    return new UpdateContextSlotSchemaItemCommand(itemId, requiredLevel, priority, active);
  }
}
