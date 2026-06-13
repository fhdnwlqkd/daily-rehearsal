package com.rehearsal.api.config.ai;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;

@Description("AI provider가 설정되지 않은 환경에서 명확한 오류를 반환하는 placeholder client")
public class UnconfiguredSlotExtractorClient implements SlotExtractorClient {

  @Override
  public SlotExtractionRawResult extract(SlotExtractionCommand command) {
    throw new IllegalStateException("AI slot extractor is not configured");
  }
}
