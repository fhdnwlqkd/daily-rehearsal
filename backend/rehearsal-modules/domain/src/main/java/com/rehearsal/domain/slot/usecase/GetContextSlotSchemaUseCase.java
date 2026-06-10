package com.rehearsal.domain.slot.usecase;

import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.usecase.command.GetContextSlotSchemaCommand;

public interface GetContextSlotSchemaUseCase {

  ContextSlotSchema getContextSlotSchema(GetContextSlotSchemaCommand command);
}
