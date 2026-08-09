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
            schema,
            new SlotExtractionRawResult(
                Map.of("desired_persona", "calm_confident", "critical_moment", "첫 인사")),
            0);

    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.missingRequiredSlotKeys()).isEmpty();
    assertThat(result.followUpQuestion()).isNull();
  }

  @Test
  void asksFollowUpWhenRequiredSlotsAreMissingAndAttemptRemains() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    SlotExtractionProcessingResult result =
        processor.process(
            schema, new SlotExtractionRawResult(Map.of("desired_persona", "calm_confident")), 0);

    assertThat(result.readyForSimulation()).isFalse();
    assertThat(result.missingRequiredSlotKeys()).containsExactly("critical_moment");
    assertThat(result.followUpQuestion()).isNotBlank();
    assertThat(result.slots().get("critical_moment").status())
        .isEqualTo(ContextSlotValueStatus.MISSING);
  }

  @Test
  void defaultsWhenRequiredSlotsAreMissingAndAttemptIsExhausted() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    SlotExtractionProcessingResult result =
        processor.process(schema, new SlotExtractionRawResult(Map.of()), 1);

    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.missingRequiredSlotKeys()).isEmpty();
    assertThat(result.slots().get("desired_persona").value()).isEqualTo("calm_confident");
    assertThat(result.slots().get("critical_moment").value()).isEqualTo("첫 인사와 가벼운 대화");
  }
}
