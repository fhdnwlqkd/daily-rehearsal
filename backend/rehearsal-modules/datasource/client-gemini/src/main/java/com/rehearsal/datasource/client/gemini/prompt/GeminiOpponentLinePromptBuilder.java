package com.rehearsal.datasource.client.gemini.prompt;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import java.util.stream.Collectors;

@Description("provider-neutral opponent line command를 Gemini system/user message로 변환하는 서비스")
public class GeminiOpponentLinePromptBuilder {

  public GeminiPromptMessages build(OpponentLineCommand command) {
    return new GeminiPromptMessages(buildSystemInstruction(), buildUserMessage(command));
  }

  private String buildSystemInstruction() {
    return """
        You play the counterpart in a rehearsal roleplay and speak the next line in Korean.
        Respond with plain text only.

        Rules:
        - Output only the opponent's next spoken line. No JSON, no labels, no quotation marks.
        - Do not include feedback, coaching comments, or judgments about the user.
        - Stay consistent with SITUATION_TYPE, FINAL_CONTEXT, and CONVERSATION_HISTORY so far.
        - Keep the line short and natural, as if spoken aloud in a single turn.
        - Do not decide whether the simulation itself should end.
        """
        .strip();
  }

  private String buildUserMessage(OpponentLineCommand command) {
    return """
        SITUATION_TYPE:
        %s

        FINAL_CONTEXT:
        %s

        SELECTED_OUTFIT:
        %s

        CONVERSATION_HISTORY:
        %s

        CURRENT_TURN:
        %d
        """
        .formatted(
            command.situationType(),
            command.finalContext(),
            command.selectedOutfitId(),
            conversationHistory(command),
            command.currentTurn())
        .strip();
  }

  private String conversationHistory(OpponentLineCommand command) {
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
}
