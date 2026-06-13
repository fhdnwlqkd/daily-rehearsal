package com.rehearsal.datasource.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.types.GenerateContentConfig;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import org.junit.jupiter.api.Test;

class GeminiSlotExtractorClientTest {

  @Test
  void extractsRawSlotsFromGeminiJsonResponse() {
    CapturingGeminiGenerateContentClient generateContentClient =
        new CapturingGeminiGenerateContentClient(
            """
            {
              "slots": {
                "situation_type": "presentation",
                "critical_moment": null
              }
            }
            """);
    GeminiSlotExtractorClient client = new GeminiSlotExtractorClient(generateContentClient);
    SlotExtractionCommand command =
        new SlotExtractionCommand(
            GeminiSlotTestFixtures.p1Schema(), "내일 발표 예상 질문이 걱정돼.", 0, SlotExtractionMode.INITIAL);

    SlotExtractionRawResult result = client.extract(command);

    assertThat(generateContentClient.model).isEqualTo(GeminiSlotExtractorClient.DEFAULT_MODEL);
    assertThat(generateContentClient.userMessage).contains("내일 발표 예상 질문이 걱정돼.");
    assertThat(generateContentClient.config.responseJsonSchema()).isPresent();
    assertThat(result.rawSlots())
        .containsEntry("situation_type", "presentation")
        .containsEntry("critical_moment", null);
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
