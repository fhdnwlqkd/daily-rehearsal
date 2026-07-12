package com.rehearsal.domain.extraction.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Description("provider와 무관하게 slot 추출 client에 전달하는 입력 계약")
public record SlotExtractionCommand(
    @Description("slot 정의와 follow-up 제한을 담은 runtime schema") ContextSlotSchemaType schema,
    @Description("사용자 음성 브리핑 또는 follow-up 답변 transcript") String transcript,
    @Description("현재까지 사용한 follow-up 질문 횟수") int followUpAttempt,
    @Description("최초 추출인지 follow-up 보강 추출인지 나타내는 mode") SlotExtractionMode mode,
    @Description("follow-up 추출 시 서버가 이미 확정한 현재 slot 값") Map<String, Object> currentSlots,
    @Description("follow-up 추출 시 우선 보강해야 하는 slot key 목록") List<String> targetSlotKeys) {

  public SlotExtractionCommand(
      ContextSlotSchemaType schema,
      String transcript,
      int followUpAttempt,
      SlotExtractionMode mode) {
    this(schema, transcript, followUpAttempt, mode, Map.of(), List.of());
  }

  public SlotExtractionCommand {
    Objects.requireNonNull(schema, "schema must not be null");
    transcript = transcript == null ? "" : transcript.strip();
    mode = mode == null ? SlotExtractionMode.INITIAL : mode;
    currentSlots = currentSlots == null ? Map.of() : Map.copyOf(currentSlots);
    targetSlotKeys = targetSlotKeys == null ? List.of() : List.copyOf(targetSlotKeys);
    if (followUpAttempt < 0) {
      throw new IllegalArgumentException("followUpAttempt must be greater than or equal to 0");
    }
  }
}
