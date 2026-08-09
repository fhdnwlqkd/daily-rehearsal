package com.rehearsal.domain.extraction.service;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.ContextSlotValueSource;
import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.extraction.service.utils.SlotSchemaItems;
import com.rehearsal.domain.extraction.service.utils.SlotValues;
import com.rehearsal.domain.slot.registry.ContextSlotOptionType;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType.SchemaItemDef;
import com.rehearsal.domain.slot.registry.ContextSlotType;
import java.util.LinkedHashMap;
import java.util.Map;

@Description("누락되거나 유효하지 않은 slot 값을 schema 기본값으로 보정해 최종 slot 맵을 확정하는 서비스")
public class FinalSlotValueResolver {

  public Map<String, ContextSlotValue> resolve(
      ContextSlotSchemaType schema, Map<String, ContextSlotValue> slots) {
    Map<String, ContextSlotValue> safeSlots = slots == null ? Map.of() : slots;
    Map<String, ContextSlotValue> resolved = new LinkedHashMap<>();

    for (SchemaItemDef item : SlotSchemaItems.activeItemsByPriority(schema)) {
      ContextSlotType slot = item.slotType();
      ContextSlotValue current = safeSlots.get(slot.getKey());
      ContextSlotValue value = current == null ? missingValue(item) : current;

      if (shouldApplyDefault(value)) {
        value = applyDefault(item, value);
      }

      resolved.put(slot.getKey(), value);
    }

    return resolved;
  }

  private boolean shouldApplyDefault(ContextSlotValue value) {
    return value.status() == ContextSlotValueStatus.MISSING
        || value.status() == ContextSlotValueStatus.INVALID;
  }

  private ContextSlotValue applyDefault(SchemaItemDef item, ContextSlotValue value) {
    ContextSlotType slot = item.slotType();
    ContextSlotOptionType defaultOption = slot.getDefaultOption();

    if (defaultOption != null) {
      return withValue(value, defaultOption.getKey(), ContextSlotValueSource.DEFAULT_OPTION);
    }

    if (!SlotValues.isEmpty(slot.getDefaultLiteralValue())) {
      return withValue(
          value, slot.getDefaultLiteralValue(), ContextSlotValueSource.DEFAULT_LITERAL);
    }

    return new ContextSlotValue(
        value.slotKey(),
        value.label(),
        value.slotType(),
        value.requiredLevel(),
        value.priority(),
        null,
        ContextSlotValueStatus.MISSING,
        ContextSlotValueSource.EMPTY);
  }

  private ContextSlotValue withValue(
      ContextSlotValue value, Object defaultValue, ContextSlotValueSource source) {
    return new ContextSlotValue(
        value.slotKey(),
        value.label(),
        value.slotType(),
        value.requiredLevel(),
        value.priority(),
        defaultValue,
        ContextSlotValueStatus.DEFAULTED,
        source);
  }

  private ContextSlotValue missingValue(SchemaItemDef item) {
    ContextSlotType slot = item.slotType();
    return new ContextSlotValue(
        slot.getKey(),
        slot.getLabel(),
        slot.getSlotType(),
        item.requiredLevel(),
        item.priority(),
        null,
        ContextSlotValueStatus.MISSING,
        ContextSlotValueSource.EMPTY);
  }
}
