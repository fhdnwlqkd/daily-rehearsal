package com.rehearsal.api.slot.controller;

import com.rehearsal.api.slot.controller.dto.AdminContextSlotListResponse;
import com.rehearsal.api.slot.controller.dto.AdminContextSlotOptionResponse;
import com.rehearsal.api.slot.controller.dto.AdminContextSlotResponse;
import com.rehearsal.api.slot.controller.dto.AdminContextSlotSchemaKeysResponse;
import com.rehearsal.api.slot.controller.dto.AdminContextSlotSchemaResponse;
import com.rehearsal.api.slot.controller.dto.CreateOptionRequest;
import com.rehearsal.api.slot.controller.dto.CreateSchemaItemRequest;
import com.rehearsal.api.slot.controller.dto.CreateSchemaRequest;
import com.rehearsal.api.slot.controller.dto.CreateSlotRequest;
import com.rehearsal.api.slot.controller.dto.UpdateOptionRequest;
import com.rehearsal.api.slot.controller.dto.UpdateSchemaItemRequest;
import com.rehearsal.api.slot.controller.dto.UpdateSchemaRequest;
import com.rehearsal.api.slot.controller.dto.UpdateSlotRequest;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.ManageContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotOptionCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotSchemaCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotSchemaItemCommand;
import com.rehearsal.domain.slot.usecase.command.GetContextSlotSchemaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class AdminContextSlotController {

  private final GetContextSlotSchemaUseCase getContextSlotSchemaUseCase;
  private final ManageContextSlotSchemaUseCase manageContextSlotSchemaUseCase;

  @GetMapping("/api/v1/admin/context-slot-schemas")
  public AdminContextSlotSchemaKeysResponse listSchemaKeys() {
    return AdminContextSlotSchemaKeysResponse.from(
        manageContextSlotSchemaUseCase.listActiveContextSlotSchemaKeys());
  }

  @GetMapping("/api/v1/admin/context-slot-schemas/{schemaKey}")
  public AdminContextSlotSchemaResponse getSchema(@PathVariable @NotBlank String schemaKey) {
    ContextSlotSchema slotSchema =
        getContextSlotSchemaUseCase.getContextSlotSchema(
            new GetContextSlotSchemaCommand(schemaKey));
    return AdminContextSlotSchemaResponse.from(slotSchema);
  }

  @PostMapping("/api/v1/admin/context-slot-schemas")
  public AdminContextSlotSchemaResponse createSchema(
      @Valid @RequestBody CreateSchemaRequest request) {
    ContextSlotSchema schema = manageContextSlotSchemaUseCase.createSchema(request.toCommand());
    return AdminContextSlotSchemaResponse.from(schema);
  }

  @PatchMapping("/api/v1/admin/context-slot-schemas/{schemaId}")
  public AdminContextSlotSchemaResponse updateSchema(
      @PathVariable @NotNull Long schemaId, @Valid @RequestBody UpdateSchemaRequest request) {
    ContextSlotSchema schema =
        manageContextSlotSchemaUseCase.updateSchema(request.toCommand(schemaId));
    return AdminContextSlotSchemaResponse.from(schema);
  }

  @DeleteMapping("/api/v1/admin/context-slot-schemas/{schemaId}")
  public void deleteSchema(@PathVariable @NotNull Long schemaId) {
    manageContextSlotSchemaUseCase.deleteSchema(new DeleteContextSlotSchemaCommand(schemaId));
  }

  @PostMapping("/api/v1/admin/context-slot-schemas/{schemaId}/items")
  public AdminContextSlotSchemaResponse createSchemaItem(
      @PathVariable @NotNull Long schemaId, @Valid @RequestBody CreateSchemaItemRequest request) {
    ContextSlotSchema schema =
        manageContextSlotSchemaUseCase.createSchemaItem(request.toCommand(schemaId));
    return AdminContextSlotSchemaResponse.from(schema);
  }

  @PatchMapping("/api/v1/admin/context-slot-schema-items/{itemId}")
  public AdminContextSlotSchemaResponse updateSchemaItem(
      @PathVariable @NotNull Long itemId, @Valid @RequestBody UpdateSchemaItemRequest request) {
    ContextSlotSchema schema =
        manageContextSlotSchemaUseCase.updateSchemaItem(request.toCommand(itemId));
    return AdminContextSlotSchemaResponse.from(schema);
  }

  @DeleteMapping("/api/v1/admin/context-slot-schema-items/{itemId}")
  public void deleteSchemaItem(@PathVariable @NotNull Long itemId) {
    manageContextSlotSchemaUseCase.deleteSchemaItem(new DeleteContextSlotSchemaItemCommand(itemId));
  }

  @GetMapping("/api/v1/admin/context-slots")
  public AdminContextSlotListResponse list() {
    return AdminContextSlotListResponse.from(manageContextSlotSchemaUseCase.listSlots());
  }

  @PostMapping("/api/v1/admin/context-slots")
  public AdminContextSlotResponse create(@Valid @RequestBody CreateSlotRequest request) {
    ContextSlot slot = manageContextSlotSchemaUseCase.createSlot(request.toCommand());
    return AdminContextSlotResponse.from(slot);
  }

  @PatchMapping("/api/v1/admin/context-slots/{slotId}")
  public AdminContextSlotResponse update(
      @PathVariable @NotNull Long slotId, @Valid @RequestBody UpdateSlotRequest request) {
    ContextSlot slot = manageContextSlotSchemaUseCase.updateSlot(request.toCommand(slotId));
    return AdminContextSlotResponse.from(slot);
  }

  @DeleteMapping("/api/v1/admin/context-slots/{slotId}")
  public void deleteSlot(@PathVariable @NotNull Long slotId) {
    manageContextSlotSchemaUseCase.deleteSlot(new DeleteContextSlotCommand(slotId));
  }

  @PostMapping("/api/v1/admin/context-slots/{slotId}/options")
  public AdminContextSlotResponse createOption(
      @PathVariable @NotNull Long slotId, @Valid @RequestBody CreateOptionRequest request) {
    ContextSlot slot = manageContextSlotSchemaUseCase.createOption(request.toCommand(slotId));
    return AdminContextSlotResponse.from(slot);
  }

  @PatchMapping("/api/v1/admin/context-slot-options/{optionId}")
  public AdminContextSlotOptionResponse updateOption(
      @PathVariable @NotNull Long optionId, @Valid @RequestBody UpdateOptionRequest request) {
    ContextSlotOption option =
        manageContextSlotSchemaUseCase.updateOption(request.toCommand(optionId));
    return AdminContextSlotOptionResponse.from(option);
  }

  @DeleteMapping("/api/v1/admin/context-slot-options/{optionId}")
  public void deleteOption(@PathVariable @NotNull Long optionId) {
    manageContextSlotSchemaUseCase.deleteOption(new DeleteContextSlotOptionCommand(optionId));
  }
}
