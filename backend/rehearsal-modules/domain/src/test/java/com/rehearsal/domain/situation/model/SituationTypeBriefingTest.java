package com.rehearsal.domain.situation.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import org.junit.jupiter.api.Test;

class SituationTypeBriefingTest {

  @Test
  void dateBriefingExplicitlyCuesAllFourRequiredContexts() {
    SituationType date = SituationType.DATE;

    assertTwoSentenceBriefing(date);
    assertThat(date.getBriefingTitle())
        .contains("내일의 소개팅", "한두 가지만")
        .contains("누구와 어디서", "인상", "대화 소재", "이번 만남에서 기대하는 점")
        .doesNotContain("모두 답하지 않아도", "한 번만", "다음 중", "무엇이든 좋아요", "차례로", "옷");
    assertThat(date.getExampleAnswer())
        .contains("친구 소개", "카페", "처음 만나요", "전시 이야기", "편안하게 대화", "다음에도 자연스럽게");
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

    assertTwoSentenceBriefing(interview);
    assertThat(interview.getBriefingTitle())
        .contains("내일의 면접", "한두 가지만")
        .contains("지원 분야", "면접 방식", "보여주고 싶은 모습", "연습하고 싶은 질문", "강점이나 경험")
        .doesNotContain("모두 답하지 않아도", "한 번만", "다음 중", "무엇이든 좋아요", "백엔드", "개발자", "프로젝트");
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

    assertTwoSentenceBriefing(firstDay);
    assertThat(firstDay.getBriefingTitle())
        .contains("내일의 첫 출근", "한두 가지만")
        .contains("함께할 팀", "맡게 될 역할", "기억되고 싶은 모습", "걱정되는 순간", "궁금한 점")
        .doesNotContain("모두 답하지 않아도", "한 번만", "다음 중", "무엇이든 좋아요");
    assertThat(firstDay.getExampleAnswer()).contains("마케팅팀", "신입", "협업적인 사람", "팀 앞", "자기소개", "걱정");
    assertThat(ContextSlotSchemaType.FIRST_DAY.getItems())
        .hasSize(15)
        .filteredOn(item -> item.requiredLevel() == RequiredLevel.REQUIRED)
        .extracting(item -> item.slotType().getKey())
        .containsExactly("situation_detail", "desired_persona", "critical_moment");
  }

  private void assertTwoSentenceBriefing(SituationType situationType) {
    assertThat(situationType.getBriefingTitle().split("(?<=[.!?])\\s+")).hasSize(2);
  }
}
