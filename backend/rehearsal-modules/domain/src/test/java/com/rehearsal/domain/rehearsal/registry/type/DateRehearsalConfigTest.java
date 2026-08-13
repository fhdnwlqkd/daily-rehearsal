package com.rehearsal.domain.rehearsal.registry.type;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.situation.model.SituationType;
import org.junit.jupiter.api.Test;

class DateRehearsalConfigTest {

  @Test
  void definesDateSpecificGenerationAndEvaluationGuidance() {
    RehearsalConfigDefinition definition = DateRehearsalConfig.definition();

    assertThat(definition.situationType()).isEqualTo(SituationType.DATE);
    assertThat(definition.maxTurn()).isEqualTo(3);
    assertThat(definition.maxAttemptsPerTurn()).isEqualTo(2);
    assertThat(definition.firstTurn().acceptedIntentHint()).contains("인사", "배려");
    assertThat(definition.turnObjectives())
        .hasSize(3)
        .element(1)
        .asString()
        .contains("sceneCue", "opponentLine", "actionPrompt", "acceptedIntentHint")
        .contains("selectedOutfit", "사용하지 않는다");
    assertThat(definition.turnObjectives().get(2))
        .contains("마무리", "다음 만남이나 연락", "감사나 작별")
        .contains("selectedOutfit", "사용하지 않는다");
    assertThat(definition.feedbackFocus())
        .contains("고칠 행동 하나", "selectedOutfit", "답변 평가나 피드백 근거로 사용하지 않는다");
    assertThat(definition.recoveryDirection()).contains("실패한 사용자 답변", "원래 목표", "selectedOutfit");
    assertThat(definition.technicalFallback().opponentLine()).isEqualTo("오늘 이렇게 이야기 나누는 건 어떠세요?");
  }
}
