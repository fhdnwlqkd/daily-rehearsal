package com.rehearsal.datasource.client.gemini;

import com.google.genai.types.GenerateContentConfig;
import com.rehearsal.datasource.client.gemini.prompt.GeminiOpponentLinePromptBuilder;
import com.rehearsal.datasource.client.gemini.prompt.GeminiPromptMessages;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import com.rehearsal.domain.rehearsal.port.OpponentLineGeneratorClient;
import lombok.RequiredArgsConstructor;

@Description("Gemini를 사용해 다음 상대 발화 순수 텍스트를 생성하는 OpponentLineGeneratorClient 구현체")
@RequiredArgsConstructor
public class GeminiOpponentLineGeneratorClient implements OpponentLineGeneratorClient {

  public static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";

  private final GeminiGenerateContentClient generateContentClient;
  private final String model;
  private final GeminiOpponentLinePromptBuilder promptBuilder;
  private final GeminiOpponentLineConfigBuilder configBuilder;

  public GeminiOpponentLineGeneratorClient(GeminiGenerateContentClient generateContentClient) {
    this(
        generateContentClient,
        DEFAULT_MODEL,
        new GeminiOpponentLinePromptBuilder(),
        new GeminiOpponentLineConfigBuilder());
  }

  @Override
  public String generate(OpponentLineCommand command) {
    GeminiPromptMessages messages = promptBuilder.build(command);
    GenerateContentConfig config = configBuilder.build(messages);
    return generateContentClient.generateContent(model, messages.userMessage(), config).strip();
  }
}
