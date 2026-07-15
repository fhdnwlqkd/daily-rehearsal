package com.rehearsal.datasource.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiSlotStructuredOutputSchemaBuilderTest {

  private final GeminiSlotStructuredOutputSchemaBuilder builder =
      new GeminiSlotStructuredOutputSchemaBuilder();

  @Test
  void buildsGeminiResponseJsonSchemaFromActiveSlots() {
    Map<String, Object> schema = builder.build(GeminiSlotTestFixtures.p1Schema());

    assertThat(schema).isEqualTo(expectedRootSchema());
  }

  private Map<String, Object> expectedRootSchema() {
    Map<String, Object> slots = objectSchema();
    Map<String, Object> slotProperties = new LinkedHashMap<>();
    slotProperties.put(
        "desired_persona", singleSelectSchema("calm_confident", "warm_natural", "sharp_prepared"));
    slotProperties.put("critical_moment", nullableStringSchema());
    slotProperties.put(
        "outfit_direction", singleSelectSchema("neat_casual", "formal_clean", "soft_friendly"));
    slots.put("properties", slotProperties);
    slots.put("required", List.of("desired_persona", "critical_moment", "outfit_direction"));

    Map<String, Object> rootProperties = new LinkedHashMap<>();
    rootProperties.put("slots", slots);

    Map<String, Object> root = objectSchema();
    root.put("properties", rootProperties);
    root.put("required", List.of("slots"));
    return root;
  }

  private Map<String, Object> objectSchema() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("additionalProperties", false);
    return schema;
  }

  private Map<String, Object> singleSelectSchema(String... optionKeys) {
    Map<String, Object> schema = nullableStringSchema();
    List<String> enumValues = new ArrayList<>(List.of(optionKeys));
    enumValues.add(null);
    schema.put("enum", enumValues);
    return schema;
  }

  private Map<String, Object> nullableStringSchema() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", List.of("string", "null"));
    return schema;
  }
}
