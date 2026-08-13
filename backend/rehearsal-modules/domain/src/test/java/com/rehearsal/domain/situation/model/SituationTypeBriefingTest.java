package com.rehearsal.domain.situation.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import org.junit.jupiter.api.Test;

class SituationTypeBriefingTest {

  @Test
  void dateBriefingExplicitlyCuesAllFourRequiredContexts() {
    SituationType date = SituationType.DATE;

    assertThat(date.getBriefingTitle())
        .contains("어떤 소개팅", "인상", "마무리", "소재", "차례로")
        .hasSizeLessThanOrEqualTo(80)
        .doesNotContain("걱정", "어려운 순간", "옷");
    assertThat(date.getExampleAnswer())
        .contains("친구 소개", "처음 만나요", "따뜻하고 자연스러운 인상", "마치면", "영화와 맛집")
        .hasSizeLessThanOrEqualTo(100);
  }

  @Test
  void everyRequiredDateContextHasAStaticFollowUpQuestion() {
    assertThat(ContextSlotSchemaType.DATE.getItems())
        .filteredOn(item -> item.requiredLevel() == RequiredLevel.REQUIRED)
        .hasSize(4)
        .allSatisfy(item -> assertThat(item.slotType().getFollowUpHint()).isNotNull().isNotBlank());
  }
}
