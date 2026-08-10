package com.rehearsal.datasource.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.types.GenerateContentConfig;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.rehearsal.model.TurnGenerationMode;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiOpponentLineGeneratorClientTest {

  @Test
  void generatesStructuredSimulationTurnPlan() {
    CapturingClient generateContentClient =
        new CapturingClient(
            """
            {"sceneCue":"면접관이 이력서를 봅니다.","opponentLine":"지원 동기를 말씀해 주세요.","actionPrompt":"지원 동기를 답해보세요.","acceptedIntentHint":"지원 이유를 설명한다."}
            """);
    GeminiOpponentLineGeneratorClient client =
        new GeminiOpponentLineGeneratorClient(generateContentClient);
    OpponentLineCommand command =
        new OpponentLineCommand(
            SituationType.INTERVIEW,
            Map.of("situation_type", "interview"),
            "outfit-id",
            List.of(),
            2,
            TurnGenerationMode.NORMAL,
            "지원 동기를 설명한다.",
            "구체성",
            "답변이 없어도 자연스럽게 다음 질문으로 전환한다.",
            null);

    SimulationTurnPlan plan = client.generate(command);

    assertThat(generateContentClient.model)
        .isEqualTo(GeminiOpponentLineGeneratorClient.DEFAULT_MODEL);
    assertThat(generateContentClient.userMessage).contains("GENERATION_MODE: NORMAL");
    assertThat(generateContentClient.config.responseMimeType()).contains("application/json");
    assertThat(generateContentClient.config.responseJsonSchema()).isPresent();
    assertThat(plan.opponentLine()).isEqualTo("지원 동기를 말씀해 주세요.");
  }

  private static class CapturingClient implements GeminiGenerateContentClient {

    private final String responseText;
    private String model;
    private String userMessage;
    private GenerateContentConfig config;

    private CapturingClient(String responseText) {
      this.responseText = responseText;
    }

    @Override
    public String generateContent(String model, String userMessage, GenerateContentConfig config) {
      this.model = model;
      this.userMessage = userMessage;
      this.config = config;
      return responseText;
    }
  }
}
