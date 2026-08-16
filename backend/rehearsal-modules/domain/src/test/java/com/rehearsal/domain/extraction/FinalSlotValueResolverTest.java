package com.rehearsal.domain.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.ContextSlotValueSource;
import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.extraction.service.ContextSlotValueNormalizer;
import com.rehearsal.domain.extraction.service.FinalSlotValueResolver;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinalSlotValueResolverTest {

  private final ContextSlotValueNormalizer normalizer = new ContextSlotValueNormalizer();
  private final FinalSlotValueResolver resolver = new FinalSlotValueResolver();

  @Test
  void appliesOnlyAnExplicitProductDefault() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized = normalizer.normalize(schema, Map.of());

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("outfit_direction").value()).isEqualTo("neat_casual");
    assertThat(finalSlots.get("outfit_direction").status())
        .isEqualTo(ContextSlotValueStatus.DEFAULTED);
    assertThat(finalSlots.get("outfit_direction").source())
        .isEqualTo(ContextSlotValueSource.DEFAULT_OPTION);
  }

  @Test
  void keepsMissingValueWhenSlotHasNoRealDefault() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized = normalizer.normalize(schema, Map.of());

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("critical_moment").value()).isNull();
    assertThat(finalSlots.get("critical_moment").status())
        .isEqualTo(ContextSlotValueStatus.MISSING);
    assertThat(finalSlots.get("critical_moment").source()).isEqualTo(ContextSlotValueSource.EMPTY);
  }

  @Test
  void invalidSelectValueBecomesMissingWhenSlotHasNoRealDefault() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized =
        normalizer.normalize(schema, Map.of("desired_persona", "invalid"));

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("desired_persona").value()).isNull();
    assertThat(finalSlots.get("desired_persona").status())
        .isEqualTo(ContextSlotValueStatus.MISSING);
    assertThat(finalSlots.get("desired_persona").source()).isEqualTo(ContextSlotValueSource.EMPTY);
  }
}
