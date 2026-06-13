package com.rehearsal.domain.extraction.port;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;

@Description("transcript에서 raw slot 값을 추출하는 외부 AI client port")
public interface SlotExtractorClient {

  SlotExtractionRawResult extract(SlotExtractionCommand command);
}
