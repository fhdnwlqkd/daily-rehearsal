package com.rehearsal.datasource.client.gemini;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.GenerateContentConfig;
import com.rehearsal.datasource.client.gemini.prompt.GeminiPromptMessages;
import com.rehearsal.datasource.client.gemini.prompt.GeminiTurnEvaluationPromptBuilder;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationCommand;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationClient;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Description("Gemini를 사용해 turn 성공/실패와 피드백을 판정하는 TurnEvaluationClient 구현체")
@RequiredArgsConstructor
public class GeminiTurnEvaluatorClient implements TurnEvaluationClient {

  public static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";

  private final GeminiGenerateContentClient generateContentClient;
  private final String model;
  private final GeminiTurnEvaluationPromptBuilder promptBuilder;
  private final GeminiTurnEvaluationConfigBuilder configBuilder;
  private final ObjectMapper objectMapper;

  public GeminiTurnEvaluatorClient(GeminiGenerateContentClient generateContentClient) {
    this(
        generateContentClient,
        DEFAULT_MODEL,
        new GeminiTurnEvaluationPromptBuilder(),
        new GeminiTurnEvaluationConfigBuilder(),
        new ObjectMapper());
  }

  @Override
  public TurnEvaluationRawResult evaluate(TurnEvaluationCommand command) {
    GeminiPromptMessages messages = promptBuilder.build(command);
    GenerateContentConfig config = configBuilder.build(messages);
    String responseText =
        generateContentClient.generateContent(model, messages.userMessage(), config);
    return parseResult(responseText);
  }

  private TurnEvaluationRawResult parseResult(String responseText) {
    try {
      Map<String, Object> response =
          objectMapper.readValue(responseText, new TypeReference<Map<String, Object>>() {});
      Object feedback = response.get("feedback");
      if (!(feedback instanceof String feedbackText) || feedbackText.isBlank()) {
        throw new IllegalStateException(
            "Gemini turn evaluation response does not contain feedback");
      }
      boolean success = Boolean.TRUE.equals(response.get("success"));
      return new TurnEvaluationRawResult(success, feedbackText);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse Gemini turn evaluation response", e);
    }
  }
}
