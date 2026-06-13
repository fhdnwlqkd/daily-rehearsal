package com.rehearsal.datasource.client.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.rehearsal.domain.core.annotation.Description;
import lombok.RequiredArgsConstructor;

@Description("Google GenAI Java SDK를 사용해 Gemini generateContent를 호출하는 client adapter")
@RequiredArgsConstructor
public class GoogleGenAiGenerateContentClient implements GeminiGenerateContentClient {

  private final Client client;

  public static GoogleGenAiGenerateContentClient fromApiKey(String apiKey) {
    return new GoogleGenAiGenerateContentClient(Client.builder().apiKey(apiKey).build());
  }

  @Override
  public String generateContent(String model, String userMessage, GenerateContentConfig config) {
    GenerateContentResponse response = client.models.generateContent(model, userMessage, config);
    String text = response.text();
    if (text == null || text.isBlank()) {
      throw new IllegalStateException("Gemini returned an empty slot extraction response");
    }
    return text;
  }
}
