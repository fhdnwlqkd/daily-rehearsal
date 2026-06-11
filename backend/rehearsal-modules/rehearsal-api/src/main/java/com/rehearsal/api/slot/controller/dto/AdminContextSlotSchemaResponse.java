package com.rehearsal.api.slot.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.model.SlotType;
import java.util.Comparator;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminContextSlotSchemaResponse(
    Long id,
    String schemaKey,
    String name,
    int maxFollowUpAttempt,
    boolean active,
    List<Item> items) {

  public static AdminContextSlotSchemaResponse from(ContextSlotSchema schema) {
    return new AdminContextSlotSchemaResponse(
        schema.id(),
        schema.schemaKey(),
        schema.name(),
        schema.maxFollowUpAttempt(),
        schema.active(),
        toItems(schema));
  }

  private static List<Item> toItems(ContextSlotSchema schema) {
    return schema.items().stream()
        .sorted(Comparator.comparingInt(ContextSlotSchemaItem::priority))
        .map(Item::from)
        .toList();
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Item(
      Long id, RequiredLevel requiredLevel, int priority, boolean active, Slot slot) {

    private static Item from(ContextSlotSchemaItem item) {
      return new Item(
          item.id(), item.requiredLevel(), item.priority(), item.active(), Slot.from(item.slot()));
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Slot(
      Long id,
      String slotKey,
      String label,
      SlotType slotType,
      String extractionHint,
      String followUpHint,
      String defaultLiteralValue,
      Option defaultOption,
      List<Option> options) {

    private static Slot from(ContextSlot slot) {
      return new Slot(
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
  }

  public record Option(Long id, String optionKey, String label) {

    private static Option from(ContextSlotOption option) {
      return new Option(option.id(), option.optionKey(), option.label());
    }
  }
}
