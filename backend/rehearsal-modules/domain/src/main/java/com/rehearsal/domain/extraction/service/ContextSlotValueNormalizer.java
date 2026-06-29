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
import com.rehearsal.domain.slot.model.SlotType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Description("원시 추출 slot 값을 schema 기준의 정형 ContextSlotValue 맵으로 변환하는 서비스")
public class ContextSlotValueNormalizer {

  public Map<String, ContextSlotValue> normalize(
      ContextSlotSchema schema, Map<String, Object> rawSlots) {
    Map<String, Object> safeRawSlots = rawSlots == null ? Map.of() : rawSlots;
    Map<String, ContextSlotValue> values = new LinkedHashMap<>();

    for (ContextSlotSchemaItem item : SlotSchemaItems.activeItemsByPriority(schema)) {
      ContextSlot slot = item.slot();
      Object rawValue = safeRawSlots.get(slot.slotKey());
      ContextSlotValueStatus status = resolveStatus(slot, rawValue);
      ContextSlotValueSource source =
          status == ContextSlotValueStatus.MISSING
              ? ContextSlotValueSource.EMPTY
              : ContextSlotValueSource.EXTRACTED;

      values.put(
          slot.slotKey(),
          new ContextSlotValue(
              slot.slotKey(),
              slot.label(),
              slot.slotType(),
              item.requiredLevel(),
              item.priority(),
              normalizeValue(rawValue),
              status,
              source));
    }

    return values;
  }

  private ContextSlotValueStatus resolveStatus(ContextSlot slot, Object rawValue) {
    if (SlotValues.isEmpty(rawValue)) {
      return ContextSlotValueStatus.MISSING;
    }

    if (slot.slotType() == SlotType.SINGLE_SELECT && !isAllowedOption(slot, rawValue)) {
      return ContextSlotValueStatus.INVALID;
    }

    return ContextSlotValueStatus.FILLED;
  }

  private boolean isAllowedOption(ContextSlot slot, Object rawValue) {
    Set<String> optionKeys =
        slot.options().stream().map(ContextSlotOption::optionKey).collect(Collectors.toSet());
    return optionKeys.contains(String.valueOf(rawValue));
  }

  private Object normalizeValue(Object rawValue) {
    if (rawValue instanceof CharSequence charSequence) {
      return charSequence.toString().trim();
    }
    return rawValue;
  }
}
