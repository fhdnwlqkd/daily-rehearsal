package com.rehearsal.domain.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.ContextSlotValueSource;
import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.extraction.service.ContextSlotValueNormalizer;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextSlotValueNormalizerTest {

  private final ContextSlotValueNormalizer normalizer = new ContextSlotValueNormalizer();

  @Test
  void normalizeBySchemaPriorityAndDropsUnknownRawKeys() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    Map<String, ContextSlotValue> values =
        normalizer.normalize(
            schema,
            Map.of(
                "critical_moment",
                " 첫 인사 ",
                "desired_persona",
                "calm_confident",
                "unknown_key",
                "ignored"));

    assertThat(values.keySet())
        .containsExactly("desired_persona", "critical_moment", "outfit_direction");
    assertThat(values.get("desired_persona").status()).isEqualTo(ContextSlotValueStatus.FILLED);
    assertThat(values.get("desired_persona").source()).isEqualTo(ContextSlotValueSource.EXTRACTED);
    assertThat(values.get("critical_moment").value()).isEqualTo("첫 인사");
    assertThat(values.get("outfit_direction").status()).isEqualTo(ContextSlotValueStatus.MISSING);
  }

  @Test
  void invalidSingleSelectValueBecomesInvalid() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    Map<String, ContextSlotValue> values =
        normalizer.normalize(schema, Map.of("desired_persona", "not_allowed"));

    assertThat(values.get("desired_persona").status()).isEqualTo(ContextSlotValueStatus.INVALID);
    assertThat(values.get("desired_persona").value()).isEqualTo("not_allowed");
  }

  @Test
  void emptyCollectionBecomesMissing() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    Map<String, ContextSlotValue> values =
        normalizer.normalize(schema, Map.of("critical_moment", List.of()));

    assertThat(values.get("critical_moment").status()).isEqualTo(ContextSlotValueStatus.MISSING);
  }
}
