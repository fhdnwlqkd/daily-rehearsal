package com.rehearsal.api.config.ai;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import com.rehearsal.domain.extraction.service.utils.SlotSchemaItems;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Description("외부 AI 호출 없이 local/test 환경에서 deterministic raw slot 값을 만드는 fake extractor")
public class FakeSlotExtractorClient implements SlotExtractorClient {

  @Override
  public SlotExtractionRawResult extract(SlotExtractionCommand command) {
    Map<String, Object> rawSlots = new LinkedHashMap<>(command.currentSlots());
    String transcript = command.transcript().strip();

    for (ContextSlotSchemaItem item : SlotSchemaItems.activeItemsByPriority(command.schema())) {
      ContextSlot slot = item.slot();
      Object value = extractValue(slot, transcript, rawSlots.get(slot.slotKey()));
      rawSlots.put(slot.slotKey(), value);
    }

    return new SlotExtractionRawResult(rawSlots);
  }

  private Object extractValue(ContextSlot slot, String transcript, Object currentValue) {
    return switch (slot.slotType()) {
      case SINGLE_SELECT -> extractSingleSelectValue(slot, transcript, currentValue);
      case TEXT -> currentValue == null ? textValue(transcript) : currentValue;
    };
  }

  private Object extractSingleSelectValue(
      ContextSlot slot, String transcript, Object currentValue) {
    String normalizedTranscript = normalize(transcript);
    for (ContextSlotOption option : slot.options()) {
      if (contains(normalizedTranscript, option.optionKey())
          || contains(normalizedTranscript, option.label())) {
        return option.optionKey();
      }
    }
    return currentValue;
  }

  private Object textValue(String transcript) {
    return transcript.isBlank() ? null : transcript;
  }

  private boolean contains(String normalizedTranscript, String value) {
    return value != null && !value.isBlank() && normalizedTranscript.contains(normalize(value));
  }

  private String normalize(String value) {
    return value.toLowerCase(Locale.ROOT);
  }
}
