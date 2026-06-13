package com.rehearsal.domain.extraction.service;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.SlotExtractionProcessingResult;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Description("원시 slot 추출 결과를 정규화하고 재질문 또는 최종 slot 확정까지 오케스트레이션하는 서비스")
@RequiredArgsConstructor
public class SlotExtractionProcessor {

  private final ContextSlotValueNormalizer normalizer;
  private final MissingRequiredSlotResolver missingRequiredSlotResolver;
  private final FollowUpQuestionResolver followUpQuestionResolver;
  private final FinalSlotValueResolver finalSlotValueResolver;

  public SlotExtractionProcessor() {
    this(
        new ContextSlotValueNormalizer(),
        new MissingRequiredSlotResolver(),
        new FollowUpQuestionResolver(),
        new FinalSlotValueResolver());
  }

  public SlotExtractionProcessingResult process(
      ContextSlotSchema schema, SlotExtractionRawResult rawResult, int followUpAttempt) {
    Map<String, ContextSlotValue> normalizedSlots =
        normalizer.normalize(schema, rawResult.rawSlots());
    List<String> missingRequiredSlotKeys = missingRequiredSlotResolver.resolve(normalizedSlots);
    String followUpQuestion =
        followUpQuestionResolver.resolve(schema, missingRequiredSlotKeys, followUpAttempt);

    if (!missingRequiredSlotKeys.isEmpty() && followUpQuestion != null) {
      return new SlotExtractionProcessingResult(
          normalizedSlots, missingRequiredSlotKeys, followUpQuestion, false);
    }

    Map<String, ContextSlotValue> finalSlots =
        finalSlotValueResolver.resolve(schema, normalizedSlots);
    List<String> remainingMissingRequiredSlotKeys = missingRequiredSlotResolver.resolve(finalSlots);

    return new SlotExtractionProcessingResult(
        finalSlots, remainingMissingRequiredSlotKeys, null, true);
  }
}
