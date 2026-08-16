package com.rehearsal.domain.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.service.ContextSlotValueNormalizer;
import com.rehearsal.domain.extraction.service.MissingRequiredSlotResolver;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MissingRequiredSlotResolverTest {

  private final ContextSlotValueNormalizer normalizer = new ContextSlotValueNormalizer();
  private final MissingRequiredSlotResolver resolver = new MissingRequiredSlotResolver();

  @Test
  void returnsOnlyRequiredMissingOrInvalidSlotsByPriority() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, Object> rawSlots =
        new LinkedHashMap<>(SlotExtractionTestFixtures.dateRequiredSlots());
    rawSlots.put("desired_persona", "invalid");
    rawSlots.put("critical_moment", "첫 인사");
    Map<String, ContextSlotValue> slots = normalizer.normalize(schema, rawSlots);

    assertThat(resolver.resolve(slots)).containsExactly("desired_persona");
  }

  @Test
  void softRequiredMissingDoesNotBlock() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> slots =
        normalizer.normalize(schema, SlotExtractionTestFixtures.dateRequiredSlots());

    assertThat(resolver.resolve(slots)).isEmpty();
    assertThat(slots.get("critical_moment").requiredLevel().name()).isEqualTo("SOFT_REQUIRED");
  }
}
