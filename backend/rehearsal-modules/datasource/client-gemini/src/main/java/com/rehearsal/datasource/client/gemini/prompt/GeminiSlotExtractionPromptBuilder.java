package com.rehearsal.datasource.client.gemini.prompt;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.service.utils.SlotSchemaItems;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import java.util.stream.Collectors;

@Description("provider-neutral slot 추출 command를 Gemini system/user message로 변환하는 서비스")
public class GeminiSlotExtractionPromptBuilder {

  public GeminiPromptMessages build(SlotExtractionCommand command) {
    return new GeminiPromptMessages(buildSystemInstruction(), buildUserMessage(command));
  }

  private String buildSystemInstruction() {
    return """
        You extract Daily Rehearsal context slots from a user transcript.
        Follow the response JSON schema exactly.

        Rules:
        - Use only slot keys supplied in SLOT_DEFINITIONS.
        - Do not invent or infer values that the user did not say clearly.
        - Return null when a slot cannot be determined.
        - For SINGLE_SELECT slots, return one allowed optionKey or null.
        - Do not create follow-up questions.
        - Do not decide missingRequiredSlotKeys or readyForSimulation.
        - The server will normalize values, detect missing required slots, build follow-up questions, and decide readiness.
        - In INITIAL mode, extract values from the full first briefing.
        - In FOLLOW_UP mode, treat the transcript as an answer to a server follow-up question.
        - In FOLLOW_UP mode, preserve CURRENT_SLOTS unless the user clearly changes a value.
        - In FOLLOW_UP mode, focus on TARGET_SLOT_KEYS first, but still capture clearly changed slot values.
        - Keep extracted free-text slot values short and reusable for later simulation prompts.
        """
        .strip();
  }

  private String buildUserMessage(SlotExtractionCommand command) {
    return """
        TRANSCRIPT:
        %s

        FOLLOW_UP_STATE:
        followUpAttempt=%d
        maxFollowUpAttempt=%d

        EXTRACTION_MODE:
        %s

        CURRENT_SLOTS:
        %s

        TARGET_SLOT_KEYS:
        %s

        SLOT_DEFINITIONS:
        %s
        """
        .formatted(
            command.transcript(),
            command.followUpAttempt(),
            command.schema().maxFollowUpAttempt(),
            command.mode(),
            command.currentSlots(),
            command.targetSlotKeys(),
            slotDefinitions(command))
        .strip();
  }

  private String slotDefinitions(SlotExtractionCommand command) {
    return SlotSchemaItems.activeItemsByPriority(command.schema()).stream()
        .map(this::slotDefinition)
        .collect(Collectors.joining("\n"));
  }

  private String slotDefinition(ContextSlotSchemaItem item) {
    ContextSlot slot = item.slot();
    return """
        - slotKey: %s
          label: %s
          slotType: %s
          requiredLevel: %s
          extractionHint: %s
          options: %s
        """
        .formatted(
            slot.slotKey(),
            slot.label(),
            slot.slotType(),
            item.requiredLevel(),
            nullToEmpty(slot.extractionHint()),
            optionKeys(slot))
        .strip();
  }

  private String optionKeys(ContextSlot slot) {
    if (slot.options() == null || slot.options().isEmpty()) {
      return "[]";
    }

    return slot.options().stream()
        .map(ContextSlotOption::optionKey)
        .collect(Collectors.joining(", ", "[", "]"));
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
