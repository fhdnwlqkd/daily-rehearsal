package com.rehearsal.api.config.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.datasource.client.gemini.GeminiGenerateContentConfigBuilder;
import com.rehearsal.datasource.client.gemini.GeminiSlotExtractorClient;
import com.rehearsal.datasource.client.gemini.GoogleGenAiGenerateContentClient;
import com.rehearsal.datasource.client.gemini.prompt.GeminiSlotExtractionPromptBuilder;
import com.rehearsal.datasource.client.openai.OpenAiResponseCreateParamsBuilder;
import com.rehearsal.datasource.client.openai.OpenAiResponsesApiClient;
import com.rehearsal.datasource.client.openai.OpenAiSlotExtractorClient;
import com.rehearsal.datasource.client.openai.prompt.SlotExtractionPromptBuilder;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Description("yml의 AI task route 설정에 따라 SlotExtractorClient bean을 구성하는 Spring 설정")
@Configuration
@EnableConfigurationProperties(AiClientProperties.class)
public class AiClientConfiguration {

  @Bean
  @ConditionalOnMissingBean(SlotExtractorClient.class)
  public SlotExtractorClient slotExtractorClient(AiClientProperties properties) {
    AiTaskRoute route = new AiTaskRouteResolver(properties).resolve(AiTask.SLOT_EXTRACTION);
    return switch (route.provider()) {
      case NONE -> new UnconfiguredSlotExtractorClient();
      case FAKE -> new FakeSlotExtractorClient();
      case OPENAI -> openAiSlotExtractorClient(properties.getOpenai(), route.model());
      case GEMINI -> geminiSlotExtractorClient(properties.getGemini(), route.model());
    };
  }

  private SlotExtractorClient openAiSlotExtractorClient(
      AiClientProperties.OpenAi properties, String model) {
    return new OpenAiSlotExtractorClient(
        OpenAiResponsesApiClient.fromApiKey(requiredApiKey("OpenAI", properties.getApiKey())),
        model,
        new SlotExtractionPromptBuilder(),
        new OpenAiResponseCreateParamsBuilder(
            properties.getMaxOutputTokens(), properties.getTemperature(), properties.isStore()),
        new ObjectMapper());
  }

  private SlotExtractorClient geminiSlotExtractorClient(
      AiClientProperties.Gemini properties, String model) {
    return new GeminiSlotExtractorClient(
        GoogleGenAiGenerateContentClient.fromApiKey(
            requiredApiKey("Gemini", properties.getApiKey())),
        model,
        new GeminiSlotExtractionPromptBuilder(),
        new GeminiGenerateContentConfigBuilder(
            properties.getTemperature(), properties.getThinkingBudget()),
        new ObjectMapper());
  }

  private String requiredApiKey(String provider, String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(provider + " api-key must be configured");
    }
    return apiKey;
  }
}
