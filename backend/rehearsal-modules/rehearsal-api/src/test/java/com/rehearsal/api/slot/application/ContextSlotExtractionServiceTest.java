package com.rehearsal.api.slot.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.slot.application.command.ExtractContextSlotsCommand;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import com.rehearsal.domain.extraction.service.SlotExtractionProcessor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextSlotExtractionServiceTest {

  @Test
  void extractsAndProcessesContextSlotsFromStaticEnumSchema() {
    SlotExtractorClient client =
        command ->
            new SlotExtractionRawResult(
                Map.of("desired_persona", "calm_confident", "critical_moment", "first greeting"));
    ContextSlotExtractionService service = service(client);

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
  void keepsMissingValuesNullAndAppliesOnlyOutfitDefaultWhenExtractorFails() {
    SlotExtractorClient client =
        command -> {
          throw new IllegalStateException("AI unavailable");
        };
    ContextSlotExtractionService service = service(client);

    ExtractContextSlotsResult result =
        service.extract(new ExtractContextSlotsCommand("tomorrow date rehearsal"));

    assertThat(result.schemaKey()).isEqualTo("date");
    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.followUpQuestion()).isNull();
    assertThat(result.missingRequiredSlotKeys())
        .containsExactly("desired_persona", "critical_moment");
    assertThat(result.slots().get("desired_persona").status())
        .isEqualTo(ContextSlotValueStatus.MISSING);
    assertThat(result.slots().get("critical_moment").status())
        .isEqualTo(ContextSlotValueStatus.MISSING);
    assertThat(result.context())
        .containsEntry("desired_persona", null)
        .containsEntry("critical_moment", null)
        .containsEntry("outfit_direction", "neat_casual");
  }

  @Test
  void acceptsMissingCurrentSlotValuesDuringFollowUpExtraction() {
    SlotExtractorClient client =
        command ->
            new SlotExtractionRawResult(
                Map.of("desired_persona", "warm_natural", "critical_moment", "first greeting"));
    Map<String, Object> currentSlots = new LinkedHashMap<>();
    currentSlots.put("desired_persona", null);
    currentSlots.put("critical_moment", "first greeting");
    currentSlots.put("outfit_direction", null);

    ExtractContextSlotsResult result =
        service(client)
            .extract(
                new ExtractContextSlotsCommand(
                    "date",
                    "warm_natural",
                    1,
                    SlotExtractionMode.FOLLOW_UP,
                    currentSlots,
                    List.of("desired_persona")));

    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.context()).containsEntry("desired_persona", "warm_natural");
  }

  private ContextSlotExtractionService service(SlotExtractorClient client) {
    return new ContextSlotExtractionService(client, new SlotExtractionProcessor());
  }
}
