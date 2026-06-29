package com.rehearsal.domain.extraction.model;

import com.rehearsal.domain.core.annotation.Description;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Description("slot 추출 처리 이후 세션 흐름에서 사용할 정형 slot 결과와 follow-up 판단 결과")
public record SlotExtractionProcessingResult(
    Map<String, ContextSlotValue> slots,
    List<String> missingRequiredSlotKeys,
    String followUpQuestion,
    boolean readyForSimulation) {

  public SlotExtractionProcessingResult {
    slots = slots == null ? Map.of() : new LinkedHashMap<>(slots);
    missingRequiredSlotKeys =
        missingRequiredSlotKeys == null ? List.of() : List.copyOf(missingRequiredSlotKeys);
  }
}
