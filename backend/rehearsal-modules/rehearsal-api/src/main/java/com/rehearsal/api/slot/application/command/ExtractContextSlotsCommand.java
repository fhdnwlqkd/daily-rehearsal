package com.rehearsal.api.slot.application.command;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import java.util.List;
import java.util.Map;

@Description("사용자 transcript에서 context slot을 추출하기 위한 application service 입력")
public record ExtractContextSlotsCommand(
    String schemaKey,
    String transcript,
    int followUpAttempt,
    SlotExtractionMode mode,
    Map<String, Object> currentSlots,
    List<String> targetSlotKeys) {

  public static final String DEFAULT_SCHEMA_KEY = "date";

  public ExtractContextSlotsCommand(String transcript) {
    this(DEFAULT_SCHEMA_KEY, transcript, 0, SlotExtractionMode.INITIAL, Map.of(), List.of());
  }

  public ExtractContextSlotsCommand {
    schemaKey = schemaKey == null || schemaKey.isBlank() ? DEFAULT_SCHEMA_KEY : schemaKey.strip();
    transcript = transcript == null ? "" : transcript.strip();
    mode = mode == null ? SlotExtractionMode.INITIAL : mode;
    currentSlots = currentSlots == null ? Map.of() : Map.copyOf(currentSlots);
    targetSlotKeys = targetSlotKeys == null ? List.of() : List.copyOf(targetSlotKeys);
    if (followUpAttempt < 0) {
      throw new IllegalArgumentException("followUpAttempt must be greater than or equal to 0");
    }
  }
}
