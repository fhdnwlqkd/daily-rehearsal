package com.rehearsal.domain.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.extraction.model.SlotExtractionProcessingResult;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.service.SlotExtractionProcessor;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SlotExtractionProcessorTest {

  private final SlotExtractionProcessor processor = new SlotExtractionProcessor();

  @Test
  void readyWhenRequiredSlotsAreFilled() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();

    SlotExtractionProcessingResult result =
        processor.process(
            schema,
            new SlotExtractionRawResult(
                Map.of(
                    "situation_type",
                    "date",
                    "desired_persona",
                    "calm_confident",
                    "critical_moment",
                    "첫 인사")),
            0);

    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.missingRequiredSlotKeys()).isEmpty();
    assertThat(result.followUpQuestion()).isNull();
  }

  @Test
  void asksFollowUpWhenRequiredSlotsAreMissingAndAttemptRemains() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();

    SlotExtractionProcessingResult result =
        processor.process(schema, new SlotExtractionRawResult(Map.of("situation_type", "date")), 0);

    assertThat(result.readyForSimulation()).isFalse();
    assertThat(result.missingRequiredSlotKeys())
        .containsExactly("desired_persona", "critical_moment");
    assertThat(result.followUpQuestion()).isNotBlank();
    assertThat(result.slots().get("desired_persona").status())
        .isEqualTo(ContextSlotValueStatus.MISSING);
  }

  @Test
  void defaultsWhenRequiredSlotsAreMissingAndAttemptIsExhausted() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();

    SlotExtractionProcessingResult result =
        processor.process(schema, new SlotExtractionRawResult(Map.of("situation_type", "date")), 1);

    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.missingRequiredSlotKeys()).isEmpty();
    assertThat(result.slots().get("desired_persona").value()).isEqualTo("calm_confident");
    assertThat(result.slots().get("critical_moment").value()).isEqualTo("첫 반응을 말해야 하는 순간");
  }
}
