package com.rehearsal.datasource.client.gemini;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.GenerateContentConfig;
import com.rehearsal.datasource.client.gemini.prompt.GeminiPromptMessages;
import com.rehearsal.datasource.client.gemini.prompt.GeminiSlotExtractionPromptBuilder;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Description("Gemini를 사용해 transcript에서 raw slot 값을 추출하는 SlotExtractorClient 구현체")
@RequiredArgsConstructor
public class GeminiSlotExtractorClient implements SlotExtractorClient {

  public static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";

  private final GeminiGenerateContentClient generateContentClient;
  private final String model;
  private final GeminiSlotExtractionPromptBuilder promptBuilder;
  private final GeminiGenerateContentConfigBuilder configBuilder;
  private final ObjectMapper objectMapper;

  public GeminiSlotExtractorClient(GeminiGenerateContentClient generateContentClient) {
    this(
        generateContentClient,
        DEFAULT_MODEL,
        new GeminiSlotExtractionPromptBuilder(),
        new GeminiGenerateContentConfigBuilder(),
        new ObjectMapper());
  }

  @Override
  public SlotExtractionRawResult extract(SlotExtractionCommand command) {
    GeminiPromptMessages messages = promptBuilder.build(command);
    GenerateContentConfig config = configBuilder.build(command.schema(), messages);
    String responseText =
        generateContentClient.generateContent(model, messages.userMessage(), config);
    return new SlotExtractionRawResult(parseRawSlots(responseText));
  }

  private Map<String, Object> parseRawSlots(String responseText) {
    try {
      Map<String, Object> response =
          objectMapper.readValue(responseText, new TypeReference<Map<String, Object>>() {});
      Object slots = response.get("slots");
      if (!(slots instanceof Map<?, ?> slotsMap)) {
        throw new IllegalStateException("Gemini slot extraction response does not contain slots");
      }

      Map<String, Object> rawSlots = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : slotsMap.entrySet()) {
        if (entry.getKey() instanceof String key) {
          rawSlots.put(key, entry.getValue());
        }
      }
      return rawSlots;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse Gemini slot extraction response", e);
    }
  }
}
