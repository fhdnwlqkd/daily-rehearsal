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
        .contains("모두 답하지 않아도", "한두 가지만", "무엇이든 좋아요", "한 번만")
        .contains("누구와 어디서", "인상", "대화 소재", "마무리")
        .doesNotContain("차례로", "옷");
    assertThat(date.getExampleAnswer())
        .contains("친구 소개", "카페", "처음 만나요", "따뜻하고 편안하게", "전시", "웃으며 마치고");
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
        .contains("모두 답하지 않아도", "한두 가지만", "무엇이든 좋아요", "한 번만")
        .contains("지원 분야", "면접 방식", "보여주고 싶은 모습", "준비하고 싶은 질문", "강점이나 경험")
        .doesNotContain("백엔드", "개발자", "프로젝트");
    assertThat(interview.getExampleAnswer())
        .contains("서비스직", "일대일", "갈등 상황", "침착하고 책임감", "걱정")
        .doesNotContain("백엔드", "개발자");
  }

  @Test
  void interviewRequiresOnlyTheThreeContextsNeededToStartPractice() {
    assertThat(ContextSlotSchemaType.INTERVIEW.getItems())
        .filteredOn(item -> item.requiredLevel() == RequiredLevel.REQUIRED)
        .extracting(item -> item.slotType().getKey())
        .containsExactly("situation_detail", "desired_persona", "critical_moment");
  }

  @Test
  void firstDayBriefingCuesEveryRequiredContextAndKeepsOptionalPromptsOptional() {
    SituationType firstDay = SituationType.FIRST_DAY;

    assertThat(firstDay.getBriefingTitle())
        .contains("모두 답하지 않아도", "한두 가지만", "무엇이든 좋아요", "한 번만")
        .contains("팀과 역할", "기억되고 싶은 모습", "걱정되는 순간", "궁금한 점");
    assertThat(firstDay.getExampleAnswer()).contains("마케팅팀", "신입", "협업적인 사람", "팀 앞", "자기소개", "걱정");
    assertThat(ContextSlotSchemaType.FIRST_DAY.getItems())
        .hasSize(15)
        .filteredOn(item -> item.requiredLevel() == RequiredLevel.REQUIRED)
        .extracting(item -> item.slotType().getKey())
        .containsExactly("situation_detail", "desired_persona", "critical_moment");
  }
}
