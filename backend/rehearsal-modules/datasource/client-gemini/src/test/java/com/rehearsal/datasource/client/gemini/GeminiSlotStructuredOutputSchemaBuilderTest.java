package com.rehearsal.datasource.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiSlotStructuredOutputSchemaBuilderTest {

  private final GeminiSlotStructuredOutputSchemaBuilder builder =
      new GeminiSlotStructuredOutputSchemaBuilder();

  @Test
  @SuppressWarnings("unchecked")
  void buildsGeminiResponseJsonSchemaFromAllActiveSlots() {
    Map<String, Object> schema = builder.build(GeminiSlotTestFixtures.p1Schema());

    assertThat(schema).containsEntry("type", "object").containsEntry("additionalProperties", false);
    Map<String, Object> rootProperties = (Map<String, Object>) schema.get("properties");
    Map<String, Object> slots = (Map<String, Object>) rootProperties.get("slots");
    Map<String, Object> slotProperties = (Map<String, Object>) slots.get("properties");

    assertThat(slotProperties.keySet())
        .containsExactly(
            "situation_detail",
            "desired_persona",
            "desired_outcome",
            "conversation_material",
            "critical_moment",
            "counterpart_context",
            "response_style",
            "familiarity_level",
            "user_strength",
            "prior_interaction_context",
            "interaction_setting",
            "supporting_example",
            "anticipated_question",
            "interaction_constraint",
            "outfit_direction");
    assertThat(slots.get("required")).isEqualTo(List.copyOf(slotProperties.keySet()));
    assertThat(schema.get("required")).isEqualTo(List.of("slots"));

    assertThat(enumValues(slotProperties, "desired_persona"))
        .containsExactly(
            "calm_confident",
            "warm_natural",
            "sharp_prepared",
            "curious_engaged",
            "honest_grounded",
            "energetic_positive",
            "thoughtful_considerate",
            "professional_reliable",
            "collaborative_open",
            null);
    assertThat(enumValues(slotProperties, "response_style"))
        .contains("concise_direct", "structured_evidence", "question_and_expand", null);
    assertThat(enumValues(slotProperties, "familiarity_level"))
        .containsExactly(
            "first_time", "limited_experience", "some_experience", "very_familiar", null);
    assertThat(enumValues(slotProperties, "outfit_direction"))
        .containsExactly("neat_casual", "formal_clean", "soft_friendly", null);
  }

  @SuppressWarnings("unchecked")
  private List<String> enumValues(Map<String, Object> slotProperties, String slotKey) {
    Map<String, Object> slotSchema = (Map<String, Object>) slotProperties.get(slotKey);
    return (List<String>) slotSchema.get("enum");
  }
}
