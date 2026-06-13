package com.rehearsal.api.slot.application.result;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Description("context slot 추출과 도메인 처리 결과를 API/session flow가 사용하기 좋은 형태로 담는 결과")
public record ExtractContextSlotsResult(
    String schemaKey,
    Map<String, Object> rawSlots,
    Map<String, ContextSlotValue> slots,
    Map<String, Object> context,
    List<String> missingRequiredSlotKeys,
    String followUpQuestion,
    boolean readyForSimulation) {

  public ExtractContextSlotsResult {
    rawSlots = rawSlots == null ? Map.of() : new LinkedHashMap<>(rawSlots);
    slots = slots == null ? Map.of() : new LinkedHashMap<>(slots);
    context = context == null ? Map.of() : new LinkedHashMap<>(context);
    missingRequiredSlotKeys =
        missingRequiredSlotKeys == null ? List.of() : List.copyOf(missingRequiredSlotKeys);
  }
}
