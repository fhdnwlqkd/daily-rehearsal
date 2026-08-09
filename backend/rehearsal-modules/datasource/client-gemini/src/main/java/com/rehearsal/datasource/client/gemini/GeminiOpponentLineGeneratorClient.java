package com.rehearsal.datasource.client.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.GenerateContentConfig;
import com.rehearsal.datasource.client.gemini.prompt.GeminiOpponentLinePromptBuilder;
import com.rehearsal.datasource.client.gemini.prompt.GeminiPromptMessages;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.rehearsal.port.OpponentLineGeneratorClient;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@Description("Gemini adapter that generates a structured simulation turn plan")
@RequiredArgsConstructor
public class GeminiOpponentLineGeneratorClient implements OpponentLineGeneratorClient {

  public static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";

  private final GeminiGenerateContentClient generateContentClient;
  private final String model;
  private final GeminiOpponentLinePromptBuilder promptBuilder;
  private final GeminiOpponentLineConfigBuilder configBuilder;
  private final ObjectMapper objectMapper;

  public GeminiOpponentLineGeneratorClient(GeminiGenerateContentClient generateContentClient) {
    this(
        generateContentClient,
        DEFAULT_MODEL,
        new GeminiOpponentLinePromptBuilder(),
        new GeminiOpponentLineConfigBuilder(),
        new ObjectMapper());
  }

  public GeminiOpponentLineGeneratorClient(
      GeminiGenerateContentClient generateContentClient,
      String model,
      GeminiOpponentLinePromptBuilder promptBuilder,
      GeminiOpponentLineConfigBuilder configBuilder) {
    this(generateContentClient, model, promptBuilder, configBuilder, new ObjectMapper());
  }

  @Override
  public SimulationTurnPlan generate(OpponentLineCommand command) {
    GeminiPromptMessages messages = promptBuilder.build(command);
    GenerateContentConfig config = configBuilder.build(messages);
    String response = generateContentClient.generateContent(model, messages.userMessage(), config);
    try {
      return objectMapper.readValue(response, SimulationTurnPlan.class);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to parse Gemini simulation turn plan", exception);
    }
  }
}
