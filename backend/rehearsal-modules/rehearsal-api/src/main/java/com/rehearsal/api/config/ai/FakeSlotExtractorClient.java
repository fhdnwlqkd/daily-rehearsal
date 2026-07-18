package com.rehearsal.api.config.ai;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import com.rehearsal.domain.extraction.service.utils.SlotSchemaItems;
import com.rehearsal.domain.slot.registry.ContextSlotOptionType;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType.SchemaItemDef;
import com.rehearsal.domain.slot.registry.ContextSlotType;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Description("외부 AI 호출 없이 local/test 환경에서 deterministic raw slot 값을 만드는 fake extractor")
public class FakeSlotExtractorClient implements SlotExtractorClient {

  @Override
  public SlotExtractionRawResult extract(SlotExtractionCommand command) {
    Map<String, Object> rawSlots = new LinkedHashMap<>(command.currentSlots());
    String transcript = command.transcript().strip();

    for (SchemaItemDef item : SlotSchemaItems.activeItemsByPriority(command.schema())) {
      ContextSlotType slot = item.slotType();
      Object value = extractValue(slot, transcript, rawSlots.get(slot.getKey()));
      rawSlots.put(slot.getKey(), value);
    }

    return new SlotExtractionRawResult(rawSlots);
  }

  private Object extractValue(ContextSlotType slot, String transcript, Object currentValue) {
    return switch (slot.getSlotType()) {
      case SINGLE_SELECT -> extractSingleSelectValue(slot, transcript, currentValue);
      case TEXT -> currentValue == null ? textValue(transcript) : currentValue;
    };
  }

  private Object extractSingleSelectValue(
      ContextSlotType slot, String transcript, Object currentValue) {
    String normalizedTranscript = normalize(transcript);
    for (ContextSlotOptionType option : slot.getOptions()) {
      if (contains(normalizedTranscript, option.getKey())
          || contains(normalizedTranscript, option.getLabel())) {
        return option.getKey();
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
