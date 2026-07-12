package com.rehearsal.domain.extraction.service;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.ContextSlotValueSource;
import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.extraction.service.utils.SlotSchemaItems;
import com.rehearsal.domain.extraction.service.utils.SlotValues;
import com.rehearsal.domain.slot.model.SlotType;
import com.rehearsal.domain.slot.registry.ContextSlotOptionType;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType.SchemaItemDef;
import com.rehearsal.domain.slot.registry.ContextSlotType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Description("원시 추출 slot 값을 schema 기준의 정형 ContextSlotValue 맵으로 변환하는 서비스")
public class ContextSlotValueNormalizer {

  public Map<String, ContextSlotValue> normalize(
      ContextSlotSchemaType schema, Map<String, Object> rawSlots) {
    Map<String, Object> safeRawSlots = rawSlots == null ? Map.of() : rawSlots;
    Map<String, ContextSlotValue> values = new LinkedHashMap<>();

    for (SchemaItemDef item : SlotSchemaItems.activeItemsByPriority(schema)) {
      ContextSlotType slot = item.slotType();
      Object rawValue = safeRawSlots.get(slot.getKey());
      ContextSlotValueStatus status = resolveStatus(slot, rawValue);
      ContextSlotValueSource source =
          status == ContextSlotValueStatus.MISSING
              ? ContextSlotValueSource.EMPTY
              : ContextSlotValueSource.EXTRACTED;

      values.put(
          slot.getKey(),
          new ContextSlotValue(
              slot.getKey(),
              slot.getLabel(),
              slot.getSlotType(),
              item.requiredLevel(),
              item.priority(),
              normalizeValue(rawValue),
              status,
              source));
    }

    return values;
  }

  private ContextSlotValueStatus resolveStatus(ContextSlotType slot, Object rawValue) {
    if (SlotValues.isEmpty(rawValue)) {
      return ContextSlotValueStatus.MISSING;
    }

    if (slot.getSlotType() == SlotType.SINGLE_SELECT && !isAllowedOption(slot, rawValue)) {
      return ContextSlotValueStatus.INVALID;
    }

    return ContextSlotValueStatus.FILLED;
  }

  private boolean isAllowedOption(ContextSlotType slot, Object rawValue) {
    Set<String> optionKeys =
        slot.getOptions().stream().map(ContextSlotOptionType::getKey).collect(Collectors.toSet());
    return optionKeys.contains(String.valueOf(rawValue));
  }

  private Object normalizeValue(Object rawValue) {
    if (rawValue instanceof CharSequence charSequence) {
      return charSequence.toString().trim();
    }
    return rawValue;
  }
}
