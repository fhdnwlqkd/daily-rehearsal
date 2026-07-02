package com.rehearsal.api.config.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.datasource.client.gemini.GeminiSlotExtractorClient;
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
  void usesGeminiProviderByDefault() {
    contextRunner
        .withPropertyValues(
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
  void failsByDefaultWhenNoApiKeyConfigured() {
    contextRunner.run(
        context ->
            assertThat(context.getStartupFailure())
                .hasRootCauseMessage("Gemini api-key must be configured"));
  }

  @Test
  void createsFakeClientFromProperties() {
    contextRunner
        .withPropertyValues("rehearsal.ai.defaults.provider=fake")
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
  void failsWhenSelectedProviderHasNoApiKey() {
    contextRunner
        .withPropertyValues("rehearsal.ai.defaults.provider=gemini")
        .run(
            context ->
                assertThat(context.getStartupFailure())
                    .hasRootCauseMessage("Gemini api-key must be configured"));
  }
}
