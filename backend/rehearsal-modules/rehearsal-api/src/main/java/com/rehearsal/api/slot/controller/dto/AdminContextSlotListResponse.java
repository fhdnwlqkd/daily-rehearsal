package com.rehearsal.api.slot.controller.dto;

import com.rehearsal.domain.slot.model.ContextSlot;
import java.util.List;

public record AdminContextSlotListResponse(List<AdminContextSlotResponse> slots) {

  public static AdminContextSlotListResponse from(List<ContextSlot> slots) {
    return new AdminContextSlotListResponse(
        slots.stream().map(AdminContextSlotResponse::from).toList());
  }
}
