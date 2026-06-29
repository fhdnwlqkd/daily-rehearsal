package com.rehearsal.datasource.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.types.GenerateContentConfig;
import com.rehearsal.datasource.client.gemini.prompt.GeminiPromptMessages;
import org.junit.jupiter.api.Test;

class GeminiGenerateContentConfigBuilderTest {

  private final GeminiGenerateContentConfigBuilder builder =
      new GeminiGenerateContentConfigBuilder();

  @Test
  void buildsJsonResponseConfigWithSchemaAndSystemInstruction() {
    GeminiPromptMessages messages = new GeminiPromptMessages("system rules", "user input");

    GenerateContentConfig config = builder.build(GeminiSlotTestFixtures.p1Schema(), messages);

    assertThat(config.responseMimeType()).hasValue("application/json");
    assertThat(config.responseJsonSchema()).isPresent();
    assertThat(config.systemInstruction()).isPresent();
    assertThat(config.temperature()).hasValue(0.0F);
    assertThat(config.thinkingConfig()).isPresent();
  }
}
