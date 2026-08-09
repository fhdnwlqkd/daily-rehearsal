package com.rehearsal.datasource.client.gemini;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.GenerateContentConfig;
import com.rehearsal.datasource.client.gemini.prompt.GeminiPromptMessages;
import com.rehearsal.datasource.client.gemini.prompt.GeminiTicketCopyPromptBuilder;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.ticket.model.ChangeCard;
import com.rehearsal.domain.ticket.model.TicketCopyRawResult;
import com.rehearsal.domain.ticket.model.TicketGenerationCommand;
import com.rehearsal.domain.ticket.port.TicketCopyGeneratorClient;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Description("Generates a ticket change card through Gemini")
@RequiredArgsConstructor
public class GeminiTicketCopyGeneratorClient implements TicketCopyGeneratorClient {

  public static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";

  private final GeminiGenerateContentClient generateContentClient;
  private final String model;
  private final GeminiTicketCopyPromptBuilder promptBuilder;
  private final GeminiTicketCopyConfigBuilder configBuilder;
  private final ObjectMapper objectMapper;

  public GeminiTicketCopyGeneratorClient(GeminiGenerateContentClient generateContentClient) {
    this(
        generateContentClient,
        DEFAULT_MODEL,
        new GeminiTicketCopyPromptBuilder(),
        new GeminiTicketCopyConfigBuilder(),
        new ObjectMapper());
  }

  @Override
  public TicketCopyRawResult generate(TicketGenerationCommand command) {
    GeminiPromptMessages messages = promptBuilder.build(command);
    GenerateContentConfig config = configBuilder.build(messages);
    String responseText =
        generateContentClient.generateContent(model, messages.userMessage(), config);
    return parseResult(responseText);
  }

  private TicketCopyRawResult parseResult(String responseText) {
    try {
      Map<String, Object> response =
          objectMapper.readValue(responseText, new TypeReference<Map<String, Object>>() {});
      Object rawChangeCard = response.get("changeCard");
      if (!(rawChangeCard instanceof Map<?, ?> changeCard)) {
        throw new IllegalStateException("Gemini ticket response does not contain changeCard");
      }
      return new TicketCopyRawResult(
          new ChangeCard(
              requiredText(changeCard, "todayAction"),
              requiredText(changeCard, "tomorrowAttitude"),
              requiredText(changeCard, "ifThenPlan")));
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to parse Gemini ticket response", exception);
    }
  }

  private String requiredText(Map<?, ?> source, String key) {
    Object value = source.get(key);
    if (!(value instanceof String text) || text.isBlank()) {
      throw new IllegalStateException("Gemini ticket response does not contain " + key);
    }
    return text;
  }
}
