package com.rehearsal.datasource.client.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.openai.models.responses.ResponseCreateParams;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import org.junit.jupiter.api.Test;

class OpenAiSlotExtractorClientTest {

  @Test
  void extractsRawSlotsFromOpenAiJsonResponse() {
    CapturingOpenAiResponsesClient responsesClient =
        new CapturingOpenAiResponsesClient(
            """
            {
              "slots": {
                "situation_type": "presentation",
                "critical_moment": null
              }
            }
            """);
    OpenAiSlotExtractorClient client = new OpenAiSlotExtractorClient(responsesClient);
    SlotExtractionCommand command =
        new SlotExtractionCommand(
            OpenAiSlotTestFixtures.p1Schema(), "내일 발표 예상 질문이 걱정돼.", 0, SlotExtractionMode.INITIAL);

    SlotExtractionRawResult result = client.extract(command);

    assertThat(responsesClient.params.model().orElseThrow().asString())
        .isEqualTo(OpenAiSlotExtractorClient.DEFAULT_MODEL);
    assertThat(responsesClient.params.input().orElseThrow().asText()).contains("내일 발표 예상 질문이 걱정돼.");
    assertThat(responsesClient.params.text()).isPresent();
    assertThat(result.rawSlots())
        .containsEntry("situation_type", "presentation")
        .containsEntry("critical_moment", null);
  }

  private static class CapturingOpenAiResponsesClient implements OpenAiResponsesClient {

    private final String responseText;
    private ResponseCreateParams params;

    private CapturingOpenAiResponsesClient(String responseText) {
      this.responseText = responseText;
    }

    @Override
    public String createResponse(ResponseCreateParams params) {
      this.params = params;
      return responseText;
    }
  }
}
