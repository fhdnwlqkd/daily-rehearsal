package com.rehearsal.domain.extraction.model;

import com.rehearsal.domain.core.annotation.Description;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Description("LLM 또는 fake extractor가 반환한 원시 slot 추출 결과를 도메인 처리 직전 형태로 담는 모델")
public record SlotExtractionRawResult(Map<String, Object> rawSlots) {

  public SlotExtractionRawResult {
    rawSlots =
        rawSlots == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(rawSlots));
  }
}
