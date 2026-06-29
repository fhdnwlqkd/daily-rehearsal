package com.rehearsal.datasource.client.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.openai.models.responses.ResponseCreateParams;
import com.rehearsal.datasource.client.openai.prompt.OpenAiPromptMessages;
import com.rehearsal.datasource.client.openai.prompt.SlotExtractionPromptBuilder;
import com.rehearsal.datasource.client.openai.prompt.SlotExtractionPromptRequest;
import org.junit.jupiter.api.Test;

class OpenAiResponseCreateParamsBuilderTest {

  private final SlotExtractionPromptBuilder promptBuilder = new SlotExtractionPromptBuilder();
  private final OpenAiResponseCreateParamsBuilder paramsBuilder =
      new OpenAiResponseCreateParamsBuilder();

  @Test
  void buildsResponseCreateParamsForSlotExtraction() {
    OpenAiPromptMessages messages =
        promptBuilder.build(
            new SlotExtractionPromptRequest(
                OpenAiSlotTestFixtures.p1Schema(), "내일 발표 예상 질문이 걱정돼.", 0));

    ResponseCreateParams params =
        paramsBuilder.build("gpt-test-model", OpenAiSlotTestFixtures.p1Schema(), messages);

    assertThat(params.model()).isPresent();
    assertThat(params.model().orElseThrow().asString()).isEqualTo("gpt-test-model");
    assertThat(params.instructions()).hasValue(messages.developerMessage());
    assertThat(params.input()).isPresent();
    assertThat(params.input().orElseThrow().asText()).contains("내일 발표 예상 질문이 걱정돼.");
    assertThat(params.text()).isPresent();
    assertThat(params.temperature()).hasValue(0.0);
    assertThat(params.maxOutputTokens()).hasValue(512L);
    assertThat(params.store()).hasValue(false);
  }
}
