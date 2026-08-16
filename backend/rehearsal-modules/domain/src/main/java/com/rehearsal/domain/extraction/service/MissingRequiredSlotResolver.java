package com.rehearsal.domain.extraction.service;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.slot.model.RequiredLevel;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Description("follow-up 대상인 누락 또는 invalid 필수 slot key를 우선순위 순서로 판정하는 서비스")
public class MissingRequiredSlotResolver {

  public List<String> resolve(Map<String, ContextSlotValue> slots) {
    if (slots == null || slots.isEmpty()) {
      return List.of();
    }

    return slots.values().stream()
        .filter(value -> value.requiredLevel() == RequiredLevel.REQUIRED)
        .filter(this::isMissingOrInvalid)
        .sorted(Comparator.comparingInt(ContextSlotValue::priority))
        .map(ContextSlotValue::slotKey)
        .toList();
  }

  private boolean isMissingOrInvalid(ContextSlotValue value) {
    return value.status() == ContextSlotValueStatus.MISSING
        || value.status() == ContextSlotValueStatus.INVALID;
  }
}
