package com.rehearsal.datasource.client.gemini.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.rehearsal.model.TurnGenerationMode;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiOpponentLinePromptBuilderTest {

  private final GeminiOpponentLinePromptBuilder builder = new GeminiOpponentLinePromptBuilder();

  @Test
  void includesSharedGroundingAndRecoveryRulesWithFullAcceptedContext() {
    OpponentLineCommand command =
        new OpponentLineCommand(
            SituationType.DATE,
            Map.of(
                "conversation_material", "전시와 산책",
                "desired_outcome", "편안한 마무리"),
            "date-neat-casual",
            List.of(new ConversationHistory(1, "기다리게 한 건 아니죠?", "저도 방금 왔어요.")),
            2,
            TurnGenerationMode.RECOVERY,
            "한 가지 소재로 상호적인 대화를 만든다.",
            "최소 의도를 우선한다.",
            "질문 범위를 좁힌다.",
            new SimulationTurnPlan(
                "대화를 이어갑니다.", "쉬는 날에는 보통 어떻게 보내세요?", "질문에 답해보세요.", "질문과 관련된 내용을 말한다."));

    GeminiPromptMessages messages = builder.build(command);

    assertThat(messages.systemInstruction())
        .contains(
            "Treat every value under FINAL_CONTEXT",
            "any phrase ending in \"제공되지 않음\"",
            "at most one question",
            "question already answered",
            "lenient minimum",
            "concrete response scaffold",
            "exactly one minimum response",
            "element unless TURN_OBJECTIVE",
            "the same elements as actionPrompt",
            "Never provide a model answer",
            "preserve the original turn goal");
    assertThat(messages.userMessage())
        .contains(
            "SITUATION_TYPE: DATE",
            "GENERATION_MODE: RECOVERY",
            "conversation_material=전시와 산책",
            "turn 1",
            "저도 방금 왔어요.",
            "질문 범위를 좁힌다.");
  }
}
