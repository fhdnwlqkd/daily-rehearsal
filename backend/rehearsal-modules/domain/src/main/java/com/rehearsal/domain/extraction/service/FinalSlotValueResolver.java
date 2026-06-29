package com.rehearsal.domain.extraction.service;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.ContextSlotValueSource;
import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.extraction.service.utils.SlotSchemaItems;
import com.rehearsal.domain.extraction.service.utils.SlotValues;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import java.util.LinkedHashMap;
import java.util.Map;

@Description("누락되거나 유효하지 않은 slot 값을 schema 기본값으로 보정해 최종 slot 맵을 확정하는 서비스")
public class FinalSlotValueResolver {

  public Map<String, ContextSlotValue> resolve(
      ContextSlotSchema schema, Map<String, ContextSlotValue> slots) {
    Map<String, ContextSlotValue> safeSlots = slots == null ? Map.of() : slots;
    Map<String, ContextSlotValue> resolved = new LinkedHashMap<>();

    for (ContextSlotSchemaItem item : SlotSchemaItems.activeItemsByPriority(schema)) {
      ContextSlot slot = item.slot();
      ContextSlotValue current = safeSlots.get(slot.slotKey());
      ContextSlotValue value = current == null ? missingValue(item) : current;

      if (shouldApplyDefault(value)) {
        value = applyDefault(item, value);
      }

      resolved.put(slot.slotKey(), value);
    }

    return resolved;
  }

  private boolean shouldApplyDefault(ContextSlotValue value) {
    return value.status() == ContextSlotValueStatus.MISSING
        || value.status() == ContextSlotValueStatus.INVALID;
  }

  private ContextSlotValue applyDefault(ContextSlotSchemaItem item, ContextSlotValue value) {
    ContextSlot slot = item.slot();
    ContextSlotOption defaultOption = slot.defaultOption();

    if (defaultOption != null) {
      return withValue(value, defaultOption.optionKey(), ContextSlotValueSource.DEFAULT_OPTION);
    }

    if (!SlotValues.isEmpty(slot.defaultLiteralValue())) {
      return withValue(value, slot.defaultLiteralValue(), ContextSlotValueSource.DEFAULT_LITERAL);
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

  private ContextSlotValue missingValue(ContextSlotSchemaItem item) {
    ContextSlot slot = item.slot();
    return new ContextSlotValue(
        slot.slotKey(),
        slot.label(),
        slot.slotType(),
        item.requiredLevel(),
        item.priority(),
        null,
        ContextSlotValueStatus.MISSING,
        ContextSlotValueSource.EMPTY);
  }
}
