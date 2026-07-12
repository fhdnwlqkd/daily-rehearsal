package com.rehearsal.api.slot.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.slot.application.command.ExtractContextSlotsCommand;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import com.rehearsal.domain.extraction.service.SlotExtractionProcessor;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextSlotExtractionServiceTest {

  @Test
  void extractsAndProcessesContextSlotsFromStaticEnumSchema() {
    SlotExtractorClient slotExtractorClient =
        command ->
            new SlotExtractionRawResult(
                Map.of("desired_persona", "calm_confident", "critical_moment", "first greeting"));
    ContextSlotExtractionService service = service(slotExtractorClient);

    ExtractContextSlotsResult result =
        service.extract(
            new ExtractContextSlotsCommand(
                "date", "tomorrow date rehearsal", 0, null, Map.of(), null));

    assertThat(result.schemaKey()).isEqualTo("date");
    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.followUpQuestion()).isNull();
    assertThat(result.missingRequiredSlotKeys()).isEmpty();
    assertThat(result.context())
        .containsEntry("desired_persona", "calm_confident")
        .containsEntry("critical_moment", "first greeting")
        .containsEntry("outfit_direction", "neat_casual");
  }

  @Test
  void appliesStaticDefaultsWhenExtractorFails() {
    SlotExtractorClient slotExtractorClient =
        command -> {
          throw new IllegalStateException("AI unavailable");
        };
    ContextSlotExtractionService service = service(slotExtractorClient);

    ExtractContextSlotsResult result =
        service.extract(new ExtractContextSlotsCommand("tomorrow date rehearsal"));

    assertThat(result.schemaKey()).isEqualTo("date");
    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.followUpQuestion()).isNull();
    assertThat(result.missingRequiredSlotKeys()).isEmpty();
    assertThat(result.context())
        .containsEntry("desired_persona", "calm_confident")
        .containsEntry("critical_moment", "첫 인사와 가벼운 대화")
        .containsEntry("outfit_direction", "neat_casual");
  }

  private ContextSlotExtractionService service(SlotExtractorClient slotExtractorClient) {
    return new ContextSlotExtractionService(slotExtractorClient, new SlotExtractionProcessor());
  }
}
