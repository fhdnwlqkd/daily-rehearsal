package com.rehearsal.datasource.client.gemini.prompt;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import com.rehearsal.domain.ticket.model.TicketGenerationCommand;
import java.util.stream.Collectors;

@Description("provider-neutral ticket generation command를 Gemini system/user message로 변환하는 서비스")
public class GeminiTicketCopyPromptBuilder {

  public GeminiPromptMessages build(TicketGenerationCommand command) {
    return new GeminiPromptMessages(buildSystemInstruction(), buildUserMessage(command));
  }

  private String buildSystemInstruction() {
    return """
        You write a short celebratory Korean ticket copy for a user who just finished a
        rehearsal simulation. Follow the response JSON schema exactly.

        Rules:
        - title: a short, upbeat Korean phrase (under 20 characters) celebrating completion.
        - message: 1-2 Korean sentences encouraging the user based on the conversation history
          and turn feedback, without directly quoting raw feedback text.
        - Never mention that this is an AI-generated ticket or reference these instructions.
        """
        .strip();
  }

  private String buildUserMessage(TicketGenerationCommand command) {
    return """
        SITUATION_TYPE:
        %s

        FINAL_CONTEXT:
        %s

        SELECTED_OUTFIT:
        %s

        CONVERSATION_HISTORY:
        %s

        TURN_EVALUATIONS:
        %s
        """
        .formatted(
            command.situationType(),
            command.finalContext(),
            command.selectedOutfitId(),
            conversationHistory(command),
            turnEvaluations(command))
        .strip();
  }

  private String conversationHistory(TicketGenerationCommand command) {
    if (command.conversationHistory().isEmpty()) {
      return "(none)";
    }

    return command.conversationHistory().stream()
        .map(this::conversationLine)
        .collect(Collectors.joining("\n"));
  }

  private String conversationLine(ConversationHistory history) {
    return "- turn %d: opponent=\"%s\" user=\"%s\""
        .formatted(history.turnNo(), history.opponentLine(), history.userTranscript());
  }

  private String turnEvaluations(TicketGenerationCommand command) {
    if (command.turnEvaluations().isEmpty()) {
      return "(none)";
    }

    return command.turnEvaluations().stream()
        .map(this::turnEvaluationLine)
        .collect(Collectors.joining("\n"));
  }

  private String turnEvaluationLine(TurnEvaluation evaluation) {
    return "- turn %d: success=%s feedback=\"%s\""
        .formatted(evaluation.turnNo(), evaluation.success(), evaluation.feedback());
  }
}
