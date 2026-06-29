package com.rehearsal.datasource.client.openai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.ResponseCreateParams;
import com.rehearsal.datasource.client.openai.prompt.OpenAiPromptMessages;
import com.rehearsal.datasource.client.openai.prompt.SlotExtractionPromptBuilder;
import com.rehearsal.datasource.client.openai.prompt.SlotExtractionPromptRequest;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Description("OpenAI를 사용해 transcript에서 raw slot 값을 추출하는 SlotExtractorClient 구현체")
@RequiredArgsConstructor
public class OpenAiSlotExtractorClient implements SlotExtractorClient {

  public static final String DEFAULT_MODEL = "gpt-5.4-nano";

  private final OpenAiResponsesClient responsesClient;
  private final String model;
  private final SlotExtractionPromptBuilder promptBuilder;
  private final OpenAiResponseCreateParamsBuilder paramsBuilder;
  private final ObjectMapper objectMapper;

  public OpenAiSlotExtractorClient(OpenAiResponsesClient responsesClient) {
    this(
        responsesClient,
        DEFAULT_MODEL,
        new SlotExtractionPromptBuilder(),
        new OpenAiResponseCreateParamsBuilder(),
        new ObjectMapper());
  }

  @Override
  public SlotExtractionRawResult extract(SlotExtractionCommand command) {
    SlotExtractionPromptRequest request = SlotExtractionPromptRequest.from(command);
    OpenAiPromptMessages messages = promptBuilder.build(request);
    ResponseCreateParams params = paramsBuilder.build(model, command.schema(), messages);
    String responseText = responsesClient.createResponse(params);
    return new SlotExtractionRawResult(parseRawSlots(responseText));
  }

  private Map<String, Object> parseRawSlots(String responseText) {
    try {
      Map<String, Object> response =
          objectMapper.readValue(responseText, new TypeReference<Map<String, Object>>() {});
      Object slots = response.get("slots");
      if (!(slots instanceof Map<?, ?> slotsMap)) {
        throw new IllegalStateException("OpenAI slot extraction response does not contain slots");
      }

      Map<String, Object> rawSlots = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : slotsMap.entrySet()) {
        if (entry.getKey() instanceof String key) {
          rawSlots.put(key, entry.getValue());
        }
      }
      return rawSlots;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse OpenAI slot extraction response", e);
    }
  }
}
