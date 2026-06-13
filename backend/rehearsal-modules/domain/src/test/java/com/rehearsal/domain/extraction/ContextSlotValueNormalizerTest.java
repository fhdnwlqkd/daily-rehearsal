package com.rehearsal.domain.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.ContextSlotValueSource;
import com.rehearsal.domain.extraction.model.ContextSlotValueStatus;
import com.rehearsal.domain.extraction.service.ContextSlotValueNormalizer;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextSlotValueNormalizerTest {

  private final ContextSlotValueNormalizer normalizer = new ContextSlotValueNormalizer();

  @Test
  void normalizeBySchemaPriorityAndDropsUnknownRawKeys() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();

    Map<String, ContextSlotValue> values =
        normalizer.normalize(
            schema,
            Map.of(
                "critical_moment", " 첫 인사 ", "situation_type", "date", "unknown_key", "ignored"));

    assertThat(values.keySet())
        .containsExactly(
            "situation_type",
            "desired_persona",
            "critical_moment",
            "anxiety_point",
            "place_context");
    assertThat(values.get("situation_type").status()).isEqualTo(ContextSlotValueStatus.FILLED);
    assertThat(values.get("situation_type").source()).isEqualTo(ContextSlotValueSource.EXTRACTED);
    assertThat(values.get("critical_moment").value()).isEqualTo("첫 인사");
    assertThat(values.get("desired_persona").status()).isEqualTo(ContextSlotValueStatus.MISSING);
  }

  @Test
  void invalidSingleSelectValueBecomesInvalid() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();

    Map<String, ContextSlotValue> values =
        normalizer.normalize(schema, Map.of("situation_type", "not_allowed"));

    assertThat(values.get("situation_type").status()).isEqualTo(ContextSlotValueStatus.INVALID);
    assertThat(values.get("situation_type").value()).isEqualTo("not_allowed");
  }

  @Test
  void emptyCollectionBecomesMissing() {
    ContextSlotSchema schema = SlotExtractionTestFixtures.p1Schema();

    Map<String, ContextSlotValue> values =
        normalizer.normalize(schema, Map.of("critical_moment", List.of()));

    assertThat(values.get("critical_moment").status()).isEqualTo(ContextSlotValueStatus.MISSING);
  }
}
