package com.rehearsal.domain.slot.registry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContextSlotTypeTest {

  @Test
  void providesAReusableFifteenSlotCatalog() {
    assertThat(ContextSlotType.values())
        .extracting(ContextSlotType::getKey)
        .containsExactly(
            "situation_detail",
            "desired_persona",
            "desired_outcome",
            "conversation_material",
            "critical_moment",
            "counterpart_context",
            "interaction_setting",
            "prior_interaction_context",
            "user_strength",
            "supporting_example",
            "anticipated_question",
            "response_style",
            "interaction_constraint",
            "familiarity_level",
            "outfit_direction");
    assertThat(ContextSlotType.values())
        .allSatisfy(
            slot -> {
              assertThat(slot.getExtractionHint()).isNotBlank();
              assertThat(slot.getFollowUpHint()).isNotBlank();
            });
  }

  @Test
  void providesRichSingleSelectOptionsWhileKeepingOutfitKeysStable() {
    assertThat(ContextSlotType.DESIRED_PERSONA.getOptions()).hasSize(9);
    assertThat(ContextSlotType.RESPONSE_STYLE.getOptions()).hasSize(8);
    assertThat(ContextSlotType.FAMILIARITY_LEVEL.getOptions()).hasSize(4);
    assertThat(ContextSlotType.OUTFIT_DIRECTION.getOptions())
        .extracting(ContextSlotOptionType::getKey)
        .containsExactly("neat_casual", "formal_clean", "soft_friendly");
  }
}
