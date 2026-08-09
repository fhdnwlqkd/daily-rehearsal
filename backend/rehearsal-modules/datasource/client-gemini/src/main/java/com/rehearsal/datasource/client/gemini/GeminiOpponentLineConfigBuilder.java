package com.rehearsal.datasource.client.gemini;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.rehearsal.datasource.client.gemini.prompt.GeminiPromptMessages;
import com.rehearsal.domain.core.annotation.Description;

@Description("Gemini next opponent line 요청에 사용할 순수 텍스트 generateContent config를 생성하는 서비스")
public class GeminiOpponentLineConfigBuilder {

  public static final float DEFAULT_TEMPERATURE = 0.0F;
  public static final int DEFAULT_THINKING_BUDGET = 0;

  private final float temperature;
  private final int thinkingBudget;

  public GeminiOpponentLineConfigBuilder() {
    this(DEFAULT_TEMPERATURE, DEFAULT_THINKING_BUDGET);
  }

  public GeminiOpponentLineConfigBuilder(float temperature, int thinkingBudget) {
    this.temperature = temperature;
    this.thinkingBudget = thinkingBudget;
  }

  public GenerateContentConfig build(GeminiPromptMessages messages) {
    return GenerateContentConfig.builder()
        .systemInstruction(Content.fromParts(Part.fromText(messages.systemInstruction())))
        .temperature(temperature)
        .thinkingConfig(ThinkingConfig.builder().thinkingBudget(thinkingBudget).build())
        .build();
  }
}
