package com.rehearsal.api.config.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FakeSlotExtractorClientTest {

  private final FakeSlotExtractorClient client = new FakeSlotExtractorClient();

  @Test
  void extractsDeterministicSlotsFromTranscript() {
    SlotExtractionCommand command =
        new SlotExtractionCommand(
            ContextSlotSchemaType.DATE, "차분하고 자신감 있게", 0, SlotExtractionMode.INITIAL);

    SlotExtractionRawResult result = client.extract(command);

    assertThat(result.rawSlots())
        .containsEntry("desired_persona", "calm_confident")
        .containsEntry("critical_moment", "차분하고 자신감 있게");
  }

  @Test
  void preservesCurrentValueWhenTranscriptDoesNotContainNewSelectValue() {
    SlotExtractionCommand command =
        new SlotExtractionCommand(
            ContextSlotSchemaType.DATE,
            "추가 답변이에요.",
            1,
            SlotExtractionMode.FOLLOW_UP,
            Map.of("desired_persona", "calm_confident"),
            List.of("critical_moment"));

    SlotExtractionRawResult result = client.extract(command);

    assertThat(result.rawSlots()).containsEntry("desired_persona", "calm_confident");
  }
}
