package com.rehearsal.datasource.client.gemini.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationCommand;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiTurnEvaluationPromptBuilderTest {

  private final GeminiTurnEvaluationPromptBuilder builder = new GeminiTurnEvaluationPromptBuilder();

  @Test
  void makesGeneratedMinimumIntentAuthoritativeOverPersonaAndMetrics() {
    TurnEvaluationCommand command =
        new TurnEvaluationCommand(
            SituationType.FIRST_DAY,
            Map.of("desired_persona", "collaborative_open"),
            "first-day-formal",
            List.of(new ConversationHistory(1, "소개해주시겠어요?", "안녕하세요. 백엔드 개발자입니다.")),
            2,
            "팀원이 말을 이어갑니다.",
            "궁금한 점은 없었어요?",
            "필요한 도움 한 가지를 말해보세요.",
            "필요한 정보나 도움 중 하나를 말하면 통과한다.",
            "협업적인 태도를 코칭한다.",
            "개발 환경을 어디서 확인하면 될까요?",
            new TurnMetrics(1200, 2.1, 0.7));

    GeminiPromptMessages messages = builder.build(command);

    assertThat(messages.systemInstruction())
        .contains(
            "Treat FINAL_CONTEXT",
            "any phrase ending in \"제공되지 않음\"",
            "Evaluate semantic intent",
            "satisfying any one alternative is enough",
            "Do not add requirements from FINAL_CONTEXT",
            "only one immediately actionable change");
    assertThat(messages.userMessage())
        .contains(
            "desired_persona=collaborative_open",
            "필요한 정보나 도움 중 하나를 말하면 통과한다.",
            "개발 환경을 어디서 확인하면 될까요?",
            "responseDelayMs=1200");
  }
}
