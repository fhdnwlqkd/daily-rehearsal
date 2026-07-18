package com.rehearsal.api.slot.application.command;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    currentSlots = immutableCopyAllowingNullValues(currentSlots);
    targetSlotKeys = targetSlotKeys == null ? List.of() : List.copyOf(targetSlotKeys);
    if (followUpAttempt < 0) {
      throw new IllegalArgumentException("followUpAttempt must be greater than or equal to 0");
    }
  }

  private static Map<String, Object> immutableCopyAllowingNullValues(Map<String, Object> values) {
    if (values == null || values.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> copied = new LinkedHashMap<>();
    values.forEach((key, value) -> copied.put(Objects.requireNonNull(key), value));
    return Collections.unmodifiableMap(copied);
  }
}
