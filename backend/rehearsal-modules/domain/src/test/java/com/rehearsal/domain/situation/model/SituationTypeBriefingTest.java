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

  @Test
  void interviewBriefingUsesGeneralInterviewLanguage() {
    SituationType interview = SituationType.INTERVIEW;

    assertThat(interview.getBriefingTitle())
        .contains("어떤 면접", "걱정되는 질문이나 순간", "남기고 싶은 인상")
        .doesNotContain("백엔드", "개발자", "프로젝트");
    assertThat(interview.getExampleAnswer())
        .contains("서비스직", "갈등 상황", "침착하고 책임감")
        .doesNotContain("백엔드", "개발자");
  }

  @Test
  void interviewRequiresOnlyTheThreeContextsNeededToStartPractice() {
    assertThat(ContextSlotSchemaType.INTERVIEW.getItems())
        .filteredOn(item -> item.requiredLevel() == RequiredLevel.REQUIRED)
        .extracting(item -> item.slotType().getKey())
        .containsExactly("situation_detail", "desired_persona", "critical_moment");
  }
}
