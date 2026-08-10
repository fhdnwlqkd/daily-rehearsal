package com.rehearsal.datasource.client.gemini.prompt;

import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import java.util.stream.Collectors;

public class GeminiOpponentLinePromptBuilder {

  public GeminiPromptMessages build(OpponentLineCommand command) {
    return new GeminiPromptMessages(systemInstruction(), userMessage(command));
  }

  private String systemInstruction() {
    return """
        You design the next turn of a Korean rehearsal roleplay.
        Follow the response JSON schema exactly.

        Rules:
        - sceneCue briefly explains the situation immediately before the counterpart speaks.
        - opponentLine is only the counterpart's natural spoken line.
        - actionPrompt tells the user what kind of action to perform without giving a script.
        - acceptedIntentHint describes the minimum intent that counts as on-topic.
        - Keep every field concise and suitable for a voice-based exhibition.
        - Use FINAL_CONTEXT and accepted CONVERSATION_HISTORY as factual background.
        - For RECOVERY mode, continue naturally without assuming the failed user response happened.
        - Never include coaching feedback or evaluate the user.
        """
        .strip();
  }

  private String userMessage(OpponentLineCommand command) {
    return """
        SITUATION_TYPE: %s
        GENERATION_MODE: %s
        CURRENT_TURN: %d
        TURN_OBJECTIVE: %s
        FEEDBACK_FOCUS: %s
        RECOVERY_DIRECTION: %s
        FINAL_CONTEXT: %s
        SELECTED_OUTFIT: %s
        ACCEPTED_CONVERSATION_HISTORY:
        %s
        PREVIOUS_TURN_PLAN: %s
        """
        .formatted(
            command.situationType(),
            command.generationMode(),
            command.currentTurn(),
            command.turnObjective(),
            command.feedbackFocus(),
            command.recoveryDirection(),
            command.finalContext(),
            command.selectedOutfitId(),
            history(command),
            command.previousTurnPlan() == null ? "(none)" : command.previousTurnPlan())
        .strip();
  }

  private String history(OpponentLineCommand command) {
    if (command.conversationHistory().isEmpty()) {
      return "(none)";
    }
    return command.conversationHistory().stream().map(this::line).collect(Collectors.joining("\n"));
  }

  private String line(ConversationHistory history) {
    return "- turn %d: opponent=\"%s\" user=\"%s\""
        .formatted(history.turnNo(), history.opponentLine(), history.userTranscript());
  }
}
