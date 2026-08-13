package com.rehearsal.domain.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.extraction.model.SlotExtractionProcessingResult;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.service.SlotExtractionProcessor;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SlotExtractionProcessorTest {

  private final SlotExtractionProcessor processor = new SlotExtractionProcessor();

  @Test
  void readyWhenRequiredSlotsAreFilled() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    SlotExtractionProcessingResult result =
        processor.process(
            schema, new SlotExtractionRawResult(SlotExtractionTestFixtures.dateRequiredSlots()), 0);

    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.missingRequiredSlotKeys()).isEmpty();
    assertThat(result.followUpQuestion()).isNull();
  }

  @Test
  void asksFollowUpWhenRequiredSlotsAreMissingAndAttemptRemains() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    Map<String, Object> rawSlots =
        new java.util.LinkedHashMap<>(SlotExtractionTestFixtures.dateRequiredSlots());
    rawSlots.remove("conversation_material");
    SlotExtractionProcessingResult result =
        processor.process(schema, new SlotExtractionRawResult(rawSlots), 0);

    assertThat(result.readyForSimulation()).isFalse();
    assertThat(result.missingRequiredSlotKeys()).containsExactly("conversation_material");
    assertThat(result.followUpQuestion()).isNotBlank();
    assertThat(result.slots().get("conversation_material").status())
        .isEqualTo(ContextSlotValueStatus.MISSING);
  }

  @Test
  void advancesWithMissingRequiredSlotsWhenAttemptIsExhausted() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    SlotExtractionProcessingResult result =
        processor.process(schema, new SlotExtractionRawResult(Map.of()), 1);

    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.followUpQuestion()).isNull();
    assertThat(result.missingRequiredSlotKeys())
        .containsExactly(
            "situation_detail", "desired_persona", "desired_outcome", "conversation_material");
    assertThat(result.slots().get("situation_detail").value()).isNull();
    assertThat(result.slots().get("desired_persona").value()).isNull();
    assertThat(result.slots().get("desired_outcome").value()).isNull();
    assertThat(result.slots().get("conversation_material").value()).isNull();
    assertThat(result.slots().get("situation_detail").status())
        .isEqualTo(ContextSlotValueStatus.MISSING);
  }
}
