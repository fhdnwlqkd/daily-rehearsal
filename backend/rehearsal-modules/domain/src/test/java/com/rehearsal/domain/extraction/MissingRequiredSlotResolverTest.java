package com.rehearsal.domain.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.service.ContextSlotValueNormalizer;
import com.rehearsal.domain.extraction.service.MissingRequiredSlotResolver;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MissingRequiredSlotResolverTest {

  private final ContextSlotValueNormalizer normalizer = new ContextSlotValueNormalizer();
  private final MissingRequiredSlotResolver resolver = new MissingRequiredSlotResolver();

  @Test
  void returnsOnlyRequiredMissingOrInvalidSlotsByPriority() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> slots =
        normalizer.normalize(
            schema,
            Map.of("situation_type", "invalid", "desired_persona", "", "critical_moment", "첫 인사"));

    assertThat(resolver.resolve(slots)).containsExactly("situation_type", "desired_persona");
  }

  @Test
  void softRequiredMissingDoesNotBlock() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> slots =
        normalizer.normalize(
            schema,
            Map.of(
                "situation_type",
                "date",
                "desired_persona",
                "calm_confident",
                "critical_moment",
                "첫 인사"));

    assertThat(resolver.resolve(slots)).isEmpty();
  }
}
