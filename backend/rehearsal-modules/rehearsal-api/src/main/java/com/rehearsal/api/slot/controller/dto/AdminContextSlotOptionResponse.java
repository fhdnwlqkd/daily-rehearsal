package com.rehearsal.api.slot.controller.dto;

import com.rehearsal.domain.slot.model.ContextSlotOption;

public record AdminContextSlotOptionResponse(Long id, String optionKey, String label) {

  public static AdminContextSlotOptionResponse from(ContextSlotOption option) {
    return new AdminContextSlotOptionResponse(option.id(), option.optionKey(), option.label());
  }
}
