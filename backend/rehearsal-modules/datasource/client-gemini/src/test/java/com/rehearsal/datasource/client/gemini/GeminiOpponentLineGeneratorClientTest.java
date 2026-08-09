package com.rehearsal.datasource.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.types.GenerateContentConfig;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiOpponentLineGeneratorClientTest {

  @Test
  void generatesPlainTextOpponentLineWithoutJsonResponseFormat() {
    CapturingGeminiGenerateContentClient generateContentClient =
        new CapturingGeminiGenerateContentClient("오늘 발표는 잘 준비되셨나요?");
    GeminiOpponentLineGeneratorClient client =
        new GeminiOpponentLineGeneratorClient(generateContentClient);
    OpponentLineCommand command =
        new OpponentLineCommand(
            SituationType.BUSINESS_MEETING,
            Map.of("situation_type", "business_meeting"),
            "test-outfit-id",
            List.of(),
            2);

    String opponentLine = client.generate(command);

    assertThat(generateContentClient.model)
        .isEqualTo(GeminiOpponentLineGeneratorClient.DEFAULT_MODEL);
    assertThat(generateContentClient.userMessage).contains("BUSINESS_MEETING");
    assertThat(generateContentClient.config.responseMimeType()).isEmpty();
    assertThat(generateContentClient.config.responseJsonSchema()).isEmpty();
    assertThat(opponentLine).isEqualTo("오늘 발표는 잘 준비되셨나요?");
  }

  private static class CapturingGeminiGenerateContentClient implements GeminiGenerateContentClient {

    private final String responseText;
    private String model;
    private String userMessage;
    private GenerateContentConfig config;

    private CapturingGeminiGenerateContentClient(String responseText) {
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
