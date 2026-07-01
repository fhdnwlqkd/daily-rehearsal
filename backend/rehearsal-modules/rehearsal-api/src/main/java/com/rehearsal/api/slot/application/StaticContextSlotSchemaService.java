package com.rehearsal.api.slot.application;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaRegistry;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.command.GetContextSlotSchemaCommand;
import org.springframework.stereotype.Service;

@Description("Static context slot schema lookup service backed by enum/registry configuration")
@Service
public class StaticContextSlotSchemaService implements GetContextSlotSchemaUseCase {

  @Override
  public ContextSlotSchema getContextSlotSchema(GetContextSlotSchemaCommand command) {
    return ContextSlotSchemaRegistry.findByKey(command.schemaKey())
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.NOT_FOUND,
                    "Context slot schema not found. schemaKey=" + command.schemaKey()));
  }
}
