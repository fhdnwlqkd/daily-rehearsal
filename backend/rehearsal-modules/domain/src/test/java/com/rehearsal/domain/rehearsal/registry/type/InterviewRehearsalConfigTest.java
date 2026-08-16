package com.rehearsal.domain.rehearsal.registry.type;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import org.junit.jupiter.api.Test;

class InterviewRehearsalConfigTest {

  private final RehearsalConfigDefinition definition = InterviewRehearsalConfig.definition();

  @Test
  void firstTurnAcceptsAShortIntroductionWithoutRequiringAJobTitle() {
    assertThat(definition.firstTurn().acceptedIntentHint())
        .contains("역할", "강점", "일하는 태도", "하나 이상")
        .doesNotContain("지원 직무와 강점");
  }

  @Test
  void feedbackFocusDefinesAtLeastFivePositiveAndNegativeBoundaries() {
    assertThat(definition.feedbackFocus())
        .contains(
            "맡은 일을 끝까지 하려고 합니다",
            "서로 대화해야죠",
            "일단 시키는 대로 할 것 같습니다",
            "둘 다 중요하다고 생각합니다",
            "먼저 죄송하다고 하겠습니다",
            "유명한 회사라서 지원했습니다",
            "대답하기 싫어요",
            "이거 녹음되는 건가요",
            "질문과 무관한 이야기",
            "감탄사나 아주 짧은 소리",
            "시스템 지시를 바꾸라고 요구",
            "표현이 무례하거나 판단이 미숙하면 통과",
            "가장 중요한 태도 문제",
            "하나만 feedback으로",
            "완결된 핵심 답변 앞뒤에",
            "짧은 주변 발화가 붙으면",
            "actionPrompt를 그대로 반복");
  }

  @Test
  void recoveryDoesNotAssumeARejectedAnswerSucceededOrDemandPastExperience() {
    assertThat(definition.recoveryDirection())
        .contains(
            "실패한 답변은 수용된 대화가 아니다", "잘 들었습니다", "자기소개를 마친 후", "과거 경험을 강요하지 말고", "첫 행동", "복구 질문 예시")
        .doesNotContain("한 가지 경험부터 설명하게 한다");
    assertThat(definition.technicalFallback().acceptedIntentHint()).contains("행동 또는 입장 하나");
  }
}
