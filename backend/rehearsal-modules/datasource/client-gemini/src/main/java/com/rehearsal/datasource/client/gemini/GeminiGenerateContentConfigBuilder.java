package com.rehearsal.datasource.client.gemini;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.rehearsal.datasource.client.gemini.prompt.GeminiPromptMessages;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;

@Description("Gemini slot 추출 요청에 사용할 generateContent config를 생성하는 서비스")
public class GeminiGenerateContentConfigBuilder {

  private static final String JSON_MIME_TYPE = "application/json";
  public static final float DEFAULT_TEMPERATURE = 0.0F;
  public static final int DEFAULT_THINKING_BUDGET = 0;

  private final GeminiSlotStructuredOutputSchemaBuilder schemaBuilder;
  private final float temperature;
  private final int thinkingBudget;

  public GeminiGenerateContentConfigBuilder() {
    this(DEFAULT_TEMPERATURE, DEFAULT_THINKING_BUDGET);
  }

  public GeminiGenerateContentConfigBuilder(float temperature, int thinkingBudget) {
    this(new GeminiSlotStructuredOutputSchemaBuilder(), temperature, thinkingBudget);
  }

  public GeminiGenerateContentConfigBuilder(
      GeminiSlotStructuredOutputSchemaBuilder schemaBuilder,
      float temperature,
      int thinkingBudget) {
    this.schemaBuilder = schemaBuilder;
    this.temperature = temperature;
    this.thinkingBudget = thinkingBudget;
  }

  public GenerateContentConfig build(ContextSlotSchemaType schema, GeminiPromptMessages messages) {
    return GenerateContentConfig.builder()
        .systemInstruction(Content.fromParts(Part.fromText(messages.systemInstruction())))
        .responseMimeType(JSON_MIME_TYPE)
        .responseJsonSchema(schemaBuilder.build(schema))
        .temperature(temperature)
        .thinkingConfig(ThinkingConfig.builder().thinkingBudget(thinkingBudget).build())
        .build();
  }
}
