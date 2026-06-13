package com.rehearsal.datasource.client.openai.prompt;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Description("slot 추출 프롬프트를 생성하기 위해 필요한 사용자 발화와 runtime slot schema 입력")
public record SlotExtractionPromptRequest(
    @Description("slot 정의와 follow-up 제한을 담은 runtime schema") ContextSlotSchema schema,
    @Description("사용자 음성 브리핑에서 확정된 transcript") String transcript,
    @Description("현재까지 사용한 follow-up 질문 횟수") int followUpAttempt,
    @Description("최초 추출인지 follow-up 보강 추출인지 나타내는 mode") SlotExtractionMode mode,
    @Description("follow-up 추출 시 서버가 이미 확정한 현재 slot 값") Map<String, Object> currentSlots,
    @Description("follow-up 추출 시 우선 보강해야 하는 slot key 목록") List<String> targetSlotKeys) {

  public SlotExtractionPromptRequest(
      ContextSlotSchema schema, String transcript, int followUpAttempt) {
    this(schema, transcript, followUpAttempt, SlotExtractionMode.INITIAL);
  }

  public SlotExtractionPromptRequest(
      ContextSlotSchema schema, String transcript, int followUpAttempt, SlotExtractionMode mode) {
    this(schema, transcript, followUpAttempt, mode, Map.of(), List.of());
  }

  public static SlotExtractionPromptRequest from(SlotExtractionCommand command) {
    return new SlotExtractionPromptRequest(
        command.schema(),
        command.transcript(),
        command.followUpAttempt(),
        command.mode(),
        command.currentSlots(),
        command.targetSlotKeys());
  }

  public SlotExtractionPromptRequest {
    Objects.requireNonNull(schema, "schema must not be null");
    mode = mode == null ? SlotExtractionMode.INITIAL : mode;
    transcript = transcript == null ? "" : transcript.strip();
    currentSlots = currentSlots == null ? Map.of() : Map.copyOf(currentSlots);
    targetSlotKeys = targetSlotKeys == null ? List.of() : List.copyOf(targetSlotKeys);
    if (followUpAttempt < 0) {
      throw new IllegalArgumentException("followUpAttempt must be greater than or equal to 0");
    }
  }
}
