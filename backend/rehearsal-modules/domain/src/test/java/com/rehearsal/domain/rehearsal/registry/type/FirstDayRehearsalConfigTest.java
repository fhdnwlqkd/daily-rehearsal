package com.rehearsal.domain.rehearsal.registry.type;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.situation.model.SituationType;
import org.junit.jupiter.api.Test;

class FirstDayRehearsalConfigTest {

  private final RehearsalConfigDefinition definition = FirstDayRehearsalConfig.definition();

  @Test
  void definesAnAnswerableThreeTurnFirstDayArc() {
    assertThat(definition.situationType()).isEqualTo(SituationType.FIRST_DAY);
    assertThat(definition.maxTurn()).isEqualTo(3);
    assertThat(definition.maxAttemptsPerTurn()).isEqualTo(2);
    assertThat(definition.firstTurn().acceptedIntentHint()).contains("하나");
    assertThat(definition.turnObjectives().get(1))
        .contains("anticipated_question", "질문은 하나만", "완성된 업무 지식이 없다는 이유만으로")
        .contains("selectedOutfit", "사용하지 않는다");
    assertThat(definition.turnObjectives().get(2))
        .contains("관계 형성·도움 요청", "질문, 도움", "협업 의지", "하나 이상")
        .doesNotContain("돌발 질문");
  }

  @Test
  void evaluationAndRecoveryDoNotRequireUnknownCompanyInformation() {
    assertThat(definition.feedbackFocus())
        .contains("추가 통과 조건이 아니다", "고칠", "하나")
        .contains("selectedOutfit", "피드백", "근거로 사용하지 않는다");
    assertThat(definition.recoveryDirection())
        .contains("내부 정보를 알지 못한다", "쉬운 질문", "하나")
        .contains("실패한 사용자 답변", "selectedOutfit");
  }
}
