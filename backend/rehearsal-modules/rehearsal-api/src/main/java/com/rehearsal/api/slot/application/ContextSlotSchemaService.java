package com.rehearsal.api.slot.application;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.repository.ContextSlotSchemaRepository;
import com.rehearsal.domain.slot.usecase.AdminContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.command.GetContextSlotSchemaCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContextSlotSchemaService
    implements GetContextSlotSchemaUseCase, AdminContextSlotSchemaUseCase {

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

  @Override
  public List<String> listActiveContextSlotSchemaKeys() {
    return contextSlotSchemaRepository.findActiveSchemaKeys();
  }
}
