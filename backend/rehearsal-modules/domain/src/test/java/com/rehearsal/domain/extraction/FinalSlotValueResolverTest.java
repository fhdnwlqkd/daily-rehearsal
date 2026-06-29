package com.rehearsal.domain.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.ContextSlotValueSource;
import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.extraction.service.ContextSlotValueNormalizer;
import com.rehearsal.domain.extraction.service.FinalSlotValueResolver;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinalSlotValueResolverTest {

  private final ContextSlotValueNormalizer normalizer = new ContextSlotValueNormalizer();
  private final FinalSlotValueResolver resolver = new FinalSlotValueResolver();

  @Test
  void appliesDefaultOptionBeforeLiteralDefault() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized = normalizer.normalize(schema, Map.of());

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("situation_type").value()).isEqualTo("daily_reset");
    assertThat(finalSlots.get("situation_type").status())
        .isEqualTo(ContextSlotValueStatus.DEFAULTED);
    assertThat(finalSlots.get("situation_type").source())
        .isEqualTo(ContextSlotValueSource.DEFAULT_OPTION);
  }

  @Test
  void appliesDefaultLiteralWhenNoDefaultOptionExists() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized = normalizer.normalize(schema, Map.of());

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("critical_moment").value()).isEqualTo("첫 반응을 말해야 하는 순간");
    assertThat(finalSlots.get("critical_moment").status())
        .isEqualTo(ContextSlotValueStatus.DEFAULTED);
    assertThat(finalSlots.get("critical_moment").source())
        .isEqualTo(ContextSlotValueSource.DEFAULT_LITERAL);
  }

  @Test
  void appliesDefaultLiteralToSoftRequiredSlot() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized = normalizer.normalize(schema, Map.of());

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("anxiety_point").status())
        .isEqualTo(ContextSlotValueStatus.DEFAULTED);
    assertThat(finalSlots.get("anxiety_point").source())
        .isEqualTo(ContextSlotValueSource.DEFAULT_LITERAL);
  }

  @Test
  void missingSlotWithoutDefaultRemainsMissing() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized = normalizer.normalize(schema, Map.of());

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("place_context").value()).isNull();
    assertThat(finalSlots.get("place_context").status()).isEqualTo(ContextSlotValueStatus.MISSING);
    assertThat(finalSlots.get("place_context").source()).isEqualTo(ContextSlotValueSource.EMPTY);
  }

  @Test
  void invalidSelectValueIsCorrectedByDefault() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized =
        normalizer.normalize(schema, Map.of("situation_type", "invalid"));

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("situation_type").value()).isEqualTo("daily_reset");
    assertThat(finalSlots.get("situation_type").status())
        .isEqualTo(ContextSlotValueStatus.DEFAULTED);
  }
}
