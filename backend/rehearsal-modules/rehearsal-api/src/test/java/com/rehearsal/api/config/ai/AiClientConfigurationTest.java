package com.rehearsal.api.config.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.datasource.client.gemini.GeminiSlotExtractorClient;
import com.rehearsal.datasource.client.openai.OpenAiSlotExtractorClient;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import com.rehearsal.domain.extraction.service.SlotExtractionProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiClientConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(SlotExtractionProcessor.class, SlotExtractionProcessor::new)
          .withUserConfiguration(AiClientConfiguration.class);

  @Test
  void createsUnconfiguredClientByDefault() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(SlotExtractorClient.class);
          assertThat(context.getBean(SlotExtractorClient.class))
              .isInstanceOf(UnconfiguredSlotExtractorClient.class);
        });
  }

  @Test
  void createsOpenAiClientFromProperties() {
    contextRunner
        .withPropertyValues(
            "rehearsal.ai.defaults.provider=openai",
            "rehearsal.ai.openai.api-key=test-openai-key",
            "rehearsal.ai.openai.model=gpt-test-model")
        .run(
            context -> {
              assertThat(context).hasSingleBean(SlotExtractorClient.class);
              assertThat(context.getBean(SlotExtractorClient.class))
                  .isInstanceOf(OpenAiSlotExtractorClient.class);
            });
  }

  @Test
  void createsFakeClientFromProperties() {
    contextRunner
        .withPropertyValues("rehearsal.ai.tasks.slot-extraction.provider=fake")
        .run(
            context -> {
              assertThat(context).hasSingleBean(SlotExtractorClient.class);
              assertThat(context.getBean(SlotExtractorClient.class))
                  .isInstanceOf(FakeSlotExtractorClient.class);
            });
  }

  @Test
  void createsGeminiClientFromProperties() {
    contextRunner
        .withPropertyValues(
            "rehearsal.ai.defaults.provider=gemini",
            "rehearsal.ai.gemini.api-key=test-gemini-key",
            "rehearsal.ai.gemini.model=gemini-test-model")
        .run(
            context -> {
              assertThat(context).hasSingleBean(SlotExtractorClient.class);
              assertThat(context.getBean(SlotExtractorClient.class))
                  .isInstanceOf(GeminiSlotExtractorClient.class);
            });
  }

  @Test
  void createsTaskSelectedGeminiClient() {
    contextRunner
        .withPropertyValues(
            "rehearsal.ai.tasks.slot-extraction.provider=gemini",
            "rehearsal.ai.tasks.slot-extraction.model=gemini-slot-model",
            "rehearsal.ai.gemini.api-key=test-gemini-key")
        .run(
            context -> {
              assertThat(context).hasSingleBean(SlotExtractorClient.class);
              assertThat(context.getBean(SlotExtractorClient.class))
                  .isInstanceOf(GeminiSlotExtractorClient.class);
            });
  }

  @Test
  void createsTaskSelectedOpenAiClient() {
    contextRunner
        .withPropertyValues(
            "rehearsal.ai.tasks.slot-extraction.provider=openai",
            "rehearsal.ai.tasks.slot-extraction.model=openai-slot-model",
            "rehearsal.ai.openai.api-key=test-openai-key")
        .run(
            context -> {
              assertThat(context).hasSingleBean(SlotExtractorClient.class);
              assertThat(context.getBean(SlotExtractorClient.class))
                  .isInstanceOf(OpenAiSlotExtractorClient.class);
            });
  }

  @Test
  void failsWhenSelectedProviderHasNoApiKey() {
    contextRunner
        .withPropertyValues("rehearsal.ai.defaults.provider=openai")
        .run(
            context ->
                assertThat(context.getStartupFailure())
                    .hasRootCauseMessage("OpenAI api-key must be configured"));
  }

  @Test
  void failsWhenTaskSelectedProviderHasNoApiKey() {
    contextRunner
        .withPropertyValues("rehearsal.ai.tasks.slot-extraction.provider=openai")
        .run(
            context ->
                assertThat(context.getStartupFailure())
                    .hasRootCauseMessage("OpenAI api-key must be configured"));
  }
}
