package com.rehearsal.api.slot.controller.dto;

import java.util.List;

public record AdminContextSlotSchemaKeysResponse(List<String> schemaKeys) {

  public static AdminContextSlotSchemaKeysResponse from(List<String> schemaKeys) {
    return new AdminContextSlotSchemaKeysResponse(List.copyOf(schemaKeys));
  }
}
