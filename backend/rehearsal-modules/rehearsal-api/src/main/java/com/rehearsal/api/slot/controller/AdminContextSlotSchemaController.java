package com.rehearsal.api.slot.controller;

import com.rehearsal.api.slot.controller.dto.AdminContextSlotSchemaKeysResponse;
import com.rehearsal.api.slot.controller.dto.AdminContextSlotSchemaResponse;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.AdminContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.command.GetContextSlotSchemaCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/context-slot-schemas")
public class AdminContextSlotSchemaController {

  private final GetContextSlotSchemaUseCase getContextSlotSchemaUseCase;
  private final AdminContextSlotSchemaUseCase adminContextSlotSchemaUseCase;

  @GetMapping
  public AdminContextSlotSchemaKeysResponse list() {
    return AdminContextSlotSchemaKeysResponse.from(
        adminContextSlotSchemaUseCase.listActiveContextSlotSchemaKeys());
  }

  @GetMapping("/{schemaKey}")
  public AdminContextSlotSchemaResponse get(@PathVariable @NotBlank String schemaKey) {
    ContextSlotSchema slotSchema =
        getContextSlotSchemaUseCase.getContextSlotSchema(
            new GetContextSlotSchemaCommand(schemaKey));
    return AdminContextSlotSchemaResponse.from(slotSchema);
  }
}
