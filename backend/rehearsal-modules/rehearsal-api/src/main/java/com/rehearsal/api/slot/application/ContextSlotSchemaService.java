package com.rehearsal.api.slot.application;

import com.rehearsal.api.common.exception.BusinessException;
import com.rehearsal.api.common.exception.ErrorCode;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.repository.ContextSlotSchemaRepository;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.command.GetContextSlotSchemaCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContextSlotSchemaService implements GetContextSlotSchemaUseCase {

  private final ContextSlotSchemaRepository contextSlotSchemaRepository;

  @Override
  public ContextSlotSchema getContextSlotSchema(GetContextSlotSchemaCommand command) {
    return contextSlotSchemaRepository
        .findActiveBySchemaKey(command.schemaKey())
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.NOT_FOUND,
                    "Context slot schema not found. schemaKey=" + command.schemaKey()));
  }
}
