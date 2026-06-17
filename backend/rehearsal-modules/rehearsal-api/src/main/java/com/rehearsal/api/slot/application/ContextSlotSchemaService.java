package com.rehearsal.api.slot.application;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.SlotType;
import com.rehearsal.domain.slot.repository.ContextSlotSchemaRepository;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.ManageContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotCommand;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotOptionCommand;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotSchemaCommand;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotSchemaItemCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotOptionCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotSchemaCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotSchemaItemCommand;
import com.rehearsal.domain.slot.usecase.command.GetContextSlotSchemaCommand;
import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotCommand;
import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotOptionCommand;
import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotSchemaCommand;
import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotSchemaItemCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContextSlotSchemaService
    implements GetContextSlotSchemaUseCase, ManageContextSlotSchemaUseCase {

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

  @Override
  @Transactional
  public ContextSlotSchema createSchema(CreateContextSlotSchemaCommand command) {
    if (contextSlotSchemaRepository.existsBySchemaKey(command.schemaKey())) {
      throw invalidRequest(
          "Context slot schema key already exists. schemaKey=" + command.schemaKey());
    }
    return contextSlotSchemaRepository.createSchema(command.toDomain());
  }

  @Override
  @Transactional
  public ContextSlotSchema updateSchema(UpdateContextSlotSchemaCommand command) {
    return contextSlotSchemaRepository.updateSchema(command.toDomain());
  }

  @Override
  public List<ContextSlot> listSlots() {
    return contextSlotSchemaRepository.findAllSlots();
  }

  @Override
  @Transactional
  public ContextSlot createSlot(CreateContextSlotCommand command) {
    validateSlotDefaults(
        command.slotType(), command.defaultLiteralValue(), command.defaultOptionId());
    if (contextSlotSchemaRepository.existsBySlotKey(command.slotKey())) {
      throw invalidRequest("Context slot key already exists. slotKey=" + command.slotKey());
    }
    if (command.defaultOptionId() != null) {
      throw invalidRequest("defaultOptionId cannot be set while creating a slot");
    }
    return contextSlotSchemaRepository.createSlot(command.toDomain());
  }

  @Override
  @Transactional
  public ContextSlot updateSlot(UpdateContextSlotCommand command) {
    ContextSlot currentSlot = findSlot(command.slotId());
    validateSlotDefaults(
        currentSlot.slotType(), command.defaultLiteralValue(), command.defaultOptionId());
    validateDefaultOption(currentSlot, command.defaultOptionId());
    return contextSlotSchemaRepository.updateSlot(command.toDomain());
  }

  @Override
  @Transactional
  public ContextSlot createOption(CreateContextSlotOptionCommand command) {
    ContextSlot currentSlot = findSlot(command.slotId());
    if (currentSlot.slotType() != SlotType.SINGLE_SELECT) {
      throw invalidRequest("Only SINGLE_SELECT slots can have options");
    }
    if (contextSlotSchemaRepository.existsOptionKey(command.slotId(), command.optionKey())) {
      throw invalidRequest(
          "Context slot option key already exists. optionKey=" + command.optionKey());
    }
    return contextSlotSchemaRepository.createOption(command.toDomain());
  }

  @Override
  @Transactional
  public ContextSlotOption updateOption(UpdateContextSlotOptionCommand command) {
    return contextSlotSchemaRepository.updateOption(command.toDomain());
  }

  @Override
  @Transactional
  public ContextSlotSchema createSchemaItem(CreateContextSlotSchemaItemCommand command) {
    findSlot(command.slotId());
    if (contextSlotSchemaRepository.existsSchemaItem(command.schemaId(), command.slotId())) {
      throw invalidRequest("Context slot is already linked to schema. slotId=" + command.slotId());
    }
    return contextSlotSchemaRepository.createSchemaItem(command.toDomain());
  }

  @Override
  @Transactional
  public ContextSlotSchema updateSchemaItem(UpdateContextSlotSchemaItemCommand command) {
    return contextSlotSchemaRepository.updateSchemaItem(command.toDomain());
  }

  @Override
  @Transactional
  public void deleteSchema(DeleteContextSlotSchemaCommand command) {
    if (contextSlotSchemaRepository.existsSchemaItemBySchemaId(command.schemaId())) {
      throw invalidRequest("Context slot schema has linked items. schemaId=" + command.schemaId());
    }
    contextSlotSchemaRepository.deleteSchema(command.toDomain());
  }

  @Override
  @Transactional
  public void deleteSlot(DeleteContextSlotCommand command) {
    if (contextSlotSchemaRepository.existsSchemaItemBySlotId(command.slotId())) {
      throw invalidRequest("Context slot is linked to a schema. slotId=" + command.slotId());
    }
    if (contextSlotSchemaRepository.existsOptionBySlotId(command.slotId())) {
      throw invalidRequest("Context slot has options. slotId=" + command.slotId());
    }
    contextSlotSchemaRepository.deleteSlot(command.toDomain());
  }

  @Override
  @Transactional
  public void deleteOption(DeleteContextSlotOptionCommand command) {
    if (contextSlotSchemaRepository.existsSlotDefaultOption(command.optionId())) {
      throw invalidRequest(
          "Context slot option is used as a default option. optionId=" + command.optionId());
    }
    contextSlotSchemaRepository.deleteOption(command.toDomain());
  }

  @Override
  @Transactional
  public void deleteSchemaItem(DeleteContextSlotSchemaItemCommand command) {
    contextSlotSchemaRepository.deleteSchemaItem(command.toDomain());
  }

  private ContextSlot findSlot(Long slotId) {
    return contextSlotSchemaRepository
        .findSlotById(slotId)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.NOT_FOUND, "Context slot not found. slotId=" + slotId));
  }

  private void validateSlotDefaults(
      SlotType slotType, String defaultLiteralValue, Long defaultOptionId) {
    if (slotType == SlotType.SINGLE_SELECT && hasText(defaultLiteralValue)) {
      throw invalidRequest("SINGLE_SELECT slot cannot have defaultLiteralValue");
    }
    if (slotType == SlotType.TEXT && defaultOptionId != null) {
      throw invalidRequest("TEXT slot cannot have defaultOptionId");
    }
  }

  private void validateDefaultOption(ContextSlot slot, Long defaultOptionId) {
    if (defaultOptionId == null) {
      return;
    }
    ContextSlotOption option =
        contextSlotSchemaRepository
            .findOptionById(defaultOptionId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "Context slot option not found. optionId=" + defaultOptionId));
    boolean sameSlot =
        slot.options().stream().anyMatch(slotOption -> slotOption.id().equals(option.id()));
    if (!sameSlot) {
      throw invalidRequest("defaultOptionId must belong to the same slot");
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private BusinessException invalidRequest(String message) {
    return new BusinessException(ErrorCode.INVALID_REQUEST, message);
  }
}
