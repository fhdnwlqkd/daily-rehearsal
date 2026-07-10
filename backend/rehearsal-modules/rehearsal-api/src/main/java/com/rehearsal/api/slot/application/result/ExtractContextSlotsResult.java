package com.rehearsal.api.slot.application.result;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Description("Processed context slot extraction result for API/session flows")
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
