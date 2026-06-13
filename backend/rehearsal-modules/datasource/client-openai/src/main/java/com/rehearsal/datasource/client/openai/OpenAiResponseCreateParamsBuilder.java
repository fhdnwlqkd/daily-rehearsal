package com.rehearsal.datasource.client.openai;

import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseTextConfig;
import com.rehearsal.datasource.client.openai.prompt.OpenAiPromptMessages;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.slot.model.ContextSlotSchema;

@Description("OpenAI slot 추출 요청에 사용할 Responses API params를 생성하는 서비스")
public class OpenAiResponseCreateParamsBuilder {

  public static final long DEFAULT_MAX_OUTPUT_TOKENS = 512L;
  public static final double DEFAULT_TEMPERATURE = 0.0;
  public static final boolean DEFAULT_STORE = false;

  private final SlotStructuredOutputSchemaBuilder schemaBuilder;
  private final long maxOutputTokens;
  private final double temperature;
  private final boolean store;

  public OpenAiResponseCreateParamsBuilder() {
    this(DEFAULT_MAX_OUTPUT_TOKENS, DEFAULT_TEMPERATURE, DEFAULT_STORE);
  }

  public OpenAiResponseCreateParamsBuilder(
      long maxOutputTokens, double temperature, boolean store) {
    this(new SlotStructuredOutputSchemaBuilder(), maxOutputTokens, temperature, store);
  }

  public OpenAiResponseCreateParamsBuilder(
      SlotStructuredOutputSchemaBuilder schemaBuilder,
      long maxOutputTokens,
      double temperature,
      boolean store) {
    this.schemaBuilder = schemaBuilder;
    this.maxOutputTokens = maxOutputTokens;
    this.temperature = temperature;
    this.store = store;
  }

  public ResponseCreateParams build(
      String model, ContextSlotSchema schema, OpenAiPromptMessages messages) {
    return ResponseCreateParams.builder()
        .model(model)
        .instructions(messages.developerMessage())
        .input(messages.userMessage())
        .text(ResponseTextConfig.builder().format(schemaBuilder.build(schema)).build())
        .temperature(temperature)
        .maxOutputTokens(maxOutputTokens)
        .store(store)
        .build();
  }
}
