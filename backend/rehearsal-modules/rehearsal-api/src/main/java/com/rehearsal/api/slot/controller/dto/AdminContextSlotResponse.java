package com.rehearsal.api.slot.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.SlotType;
import java.util.Comparator;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminContextSlotResponse(
    Long id,
    String slotKey,
    String label,
    SlotType slotType,
    String extractionHint,
    String followUpHint,
    String defaultLiteralValue,
    Option defaultOption,
    List<Option> options) {

  public static AdminContextSlotResponse from(ContextSlot slot) {
    return new AdminContextSlotResponse(
        slot.id(),
        slot.slotKey(),
        slot.label(),
        slot.slotType(),
        slot.extractionHint(),
        slot.followUpHint(),
        slot.defaultLiteralValue(),
        slot.defaultOption() == null ? null : Option.from(slot.defaultOption()),
        toOptions(slot));
  }

  private static List<Option> toOptions(ContextSlot slot) {
    return slot.options().stream()
        .sorted(Comparator.comparing(ContextSlotOption::optionKey))
        .map(Option::from)
        .toList();
  }

  public record Option(Long id, String optionKey, String label) {

    public static Option from(ContextSlotOption option) {
      return new Option(option.id(), option.optionKey(), option.label());
    }
  }
}
