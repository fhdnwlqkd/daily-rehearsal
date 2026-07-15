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
  void appliesDefaultOptionBeforeLiteralDefault() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized = normalizer.normalize(schema, Map.of());

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("desired_persona").value()).isEqualTo("calm_confident");
    assertThat(finalSlots.get("desired_persona").status())
        .isEqualTo(ContextSlotValueStatus.DEFAULTED);
    assertThat(finalSlots.get("desired_persona").source())
        .isEqualTo(ContextSlotValueSource.DEFAULT_OPTION);
  }

  @Test
  void appliesDefaultLiteralWhenNoDefaultOptionExists() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized = normalizer.normalize(schema, Map.of());

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("critical_moment").value()).isEqualTo("첫 인사와 가벼운 대화");
    assertThat(finalSlots.get("critical_moment").status())
        .isEqualTo(ContextSlotValueStatus.DEFAULTED);
    assertThat(finalSlots.get("critical_moment").source())
        .isEqualTo(ContextSlotValueSource.DEFAULT_LITERAL);
  }

  @Test
  void invalidSelectValueIsCorrectedByDefault() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();
    Map<String, ContextSlotValue> normalized =
        normalizer.normalize(schema, Map.of("desired_persona", "invalid"));

    Map<String, ContextSlotValue> finalSlots = resolver.resolve(schema, normalized);

    assertThat(finalSlots.get("desired_persona").value()).isEqualTo("calm_confident");
    assertThat(finalSlots.get("desired_persona").status())
        .isEqualTo(ContextSlotValueStatus.DEFAULTED);
  }
}
