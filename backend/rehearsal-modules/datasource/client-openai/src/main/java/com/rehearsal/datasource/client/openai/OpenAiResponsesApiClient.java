package com.rehearsal.datasource.client.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.rehearsal.domain.core.annotation.Description;
import lombok.RequiredArgsConstructor;

@Description("OpenAI Java SDK를 사용해 Responses API를 호출하는 client adapter")
@RequiredArgsConstructor
public class OpenAiResponsesApiClient implements OpenAiResponsesClient {

  private final OpenAIClient client;

  public static OpenAiResponsesApiClient fromApiKey(String apiKey) {
    return new OpenAiResponsesApiClient(OpenAIOkHttpClient.builder().apiKey(apiKey).build());
  }

  @Override
  public String createResponse(ResponseCreateParams params) {
    Response response = client.responses().create(params);
    String responseText = outputText(response);
    if (responseText.isBlank()) {
      throw new IllegalStateException("OpenAI returned an empty slot extraction response");
    }
    return responseText;
  }

  private String outputText(Response response) {
    StringBuilder text = new StringBuilder();
    for (ResponseOutputItem outputItem : response.output()) {
      outputItem.message().ifPresent(message -> appendMessageText(text, message));
    }
    return text.toString();
  }

  private void appendMessageText(StringBuilder text, ResponseOutputMessage message) {
    for (ResponseOutputMessage.Content content : message.content()) {
      content.outputText().ifPresent(outputText -> text.append(outputText.text()));
    }
  }
}
