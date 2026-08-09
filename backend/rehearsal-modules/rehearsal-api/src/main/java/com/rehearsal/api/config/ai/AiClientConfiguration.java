package com.rehearsal.api.config.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.datasource.client.gemini.GeminiGenerateContentConfigBuilder;
import com.rehearsal.datasource.client.gemini.GeminiOpponentLineConfigBuilder;
import com.rehearsal.datasource.client.gemini.GeminiOpponentLineGeneratorClient;
import com.rehearsal.datasource.client.gemini.GeminiSlotExtractorClient;
import com.rehearsal.datasource.client.gemini.GeminiTicketCopyConfigBuilder;
import com.rehearsal.datasource.client.gemini.GeminiTicketCopyGeneratorClient;
import com.rehearsal.datasource.client.gemini.GeminiTurnEvaluationConfigBuilder;
import com.rehearsal.datasource.client.gemini.GeminiTurnEvaluatorClient;
import com.rehearsal.datasource.client.gemini.GoogleGenAiGenerateContentClient;
import com.rehearsal.datasource.client.gemini.prompt.GeminiOpponentLinePromptBuilder;
import com.rehearsal.datasource.client.gemini.prompt.GeminiSlotExtractionPromptBuilder;
import com.rehearsal.datasource.client.gemini.prompt.GeminiTicketCopyPromptBuilder;
import com.rehearsal.datasource.client.gemini.prompt.GeminiTurnEvaluationPromptBuilder;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import com.rehearsal.domain.rehearsal.port.OpponentLineGeneratorClient;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationClient;
import com.rehearsal.domain.ticket.port.TicketCopyGeneratorClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Description(
    "yml의 AI provider 설정에 따라 SlotExtractorClient/TurnEvaluationClient bean을 구성하는 Spring 설정")
@Configuration
@EnableConfigurationProperties(AiClientProperties.class)
public class AiClientConfiguration {

  @Bean
  @ConditionalOnMissingBean(SlotExtractorClient.class)
  public SlotExtractorClient slotExtractorClient(AiClientProperties properties) {
    return switch (properties.getDefaults().getProvider()) {
      case FAKE -> new FakeSlotExtractorClient();
      case GEMINI -> geminiSlotExtractorClient(properties.getGemini());
    };
  }

  @Bean
  @ConditionalOnMissingBean(TurnEvaluationClient.class)
  public TurnEvaluationClient turnEvaluationClient(AiClientProperties properties) {
    return switch (properties.getDefaults().getProvider()) {
      case FAKE -> new FakeTurnEvaluationClient();
      case GEMINI -> geminiTurnEvaluationClient(properties.getGemini());
    };
  }

  @Bean
  @ConditionalOnMissingBean(OpponentLineGeneratorClient.class)
  public OpponentLineGeneratorClient opponentLineGeneratorClient(AiClientProperties properties) {
    return switch (properties.getDefaults().getProvider()) {
      case FAKE -> new FakeOpponentLineGeneratorClient();
      case GEMINI -> geminiOpponentLineGeneratorClient(properties.getGemini());
    };
  }

  @Bean
  @ConditionalOnMissingBean(TicketCopyGeneratorClient.class)
  public TicketCopyGeneratorClient ticketCopyGeneratorClient(AiClientProperties properties) {
    return switch (properties.getDefaults().getProvider()) {
      case FAKE -> new FakeTicketCopyGeneratorClient();
      case GEMINI -> geminiTicketCopyGeneratorClient(properties.getGemini());
    };
  }

  private SlotExtractorClient geminiSlotExtractorClient(AiClientProperties.Gemini properties) {
    return new GeminiSlotExtractorClient(
        GoogleGenAiGenerateContentClient.fromApiKey(
            requiredApiKey("Gemini", properties.getApiKey())),
        properties.getModel(),
        new GeminiSlotExtractionPromptBuilder(),
        new GeminiGenerateContentConfigBuilder(
            properties.getTemperature(), properties.getThinkingBudget()),
        new ObjectMapper());
  }

  private TurnEvaluationClient geminiTurnEvaluationClient(AiClientProperties.Gemini properties) {
    return new GeminiTurnEvaluatorClient(
        GoogleGenAiGenerateContentClient.fromApiKey(
            requiredApiKey("Gemini", properties.getApiKey())),
        properties.getModel(),
        new GeminiTurnEvaluationPromptBuilder(),
        new GeminiTurnEvaluationConfigBuilder(
            properties.getTemperature(), properties.getThinkingBudget()),
        new ObjectMapper());
  }

  private OpponentLineGeneratorClient geminiOpponentLineGeneratorClient(
      AiClientProperties.Gemini properties) {
    return new GeminiOpponentLineGeneratorClient(
        GoogleGenAiGenerateContentClient.fromApiKey(
            requiredApiKey("Gemini", properties.getApiKey())),
        properties.getModel(),
        new GeminiOpponentLinePromptBuilder(),
        new GeminiOpponentLineConfigBuilder(
            properties.getTemperature(), properties.getThinkingBudget()));
  }

  private TicketCopyGeneratorClient geminiTicketCopyGeneratorClient(
      AiClientProperties.Gemini properties) {
    return new GeminiTicketCopyGeneratorClient(
        GoogleGenAiGenerateContentClient.fromApiKey(
            requiredApiKey("Gemini", properties.getApiKey())),
        properties.getModel(),
        new GeminiTicketCopyPromptBuilder(),
        new GeminiTicketCopyConfigBuilder(
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
