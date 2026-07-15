package com.rehearsal.datasource.client.gemini.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.datasource.client.gemini.GeminiSlotTestFixtures;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiSlotExtractionPromptBuilderTest {

  private final GeminiSlotExtractionPromptBuilder builder = new GeminiSlotExtractionPromptBuilder();

  @Test
  void buildsInitialModePromptFromCommand() {
    SlotExtractionCommand command =
        new SlotExtractionCommand(
            GeminiSlotTestFixtures.p1Schema(),
            "내일 발표에서 예상 질문을 받을 때 차분하게 답하고 싶어.",
            0,
            SlotExtractionMode.INITIAL);

    GeminiPromptMessages messages = builder.build(command);

    assertThat(messages.systemInstruction())
        .contains("response JSON schema", "Do not create follow-up questions", "INITIAL mode");
    assertThat(messages.userMessage())
        .contains("EXTRACTION_MODE:")
        .contains("INITIAL")
        .contains("slotKey: desired_persona")
        .contains("options: [calm_confident, warm_natural, sharp_prepared]")
        .doesNotContain("followUpHint");
  }

  @Test
  void buildsFollowUpModePromptWithCurrentSlotsAndTargets() {
    SlotExtractionCommand command =
        new SlotExtractionCommand(
            GeminiSlotTestFixtures.p1Schema(),
            "예상 질문을 받는 순간이 걱정돼.",
            1,
            SlotExtractionMode.FOLLOW_UP,
            Map.of("desired_persona", "calm_confident"),
            List.of("critical_moment"));

    GeminiPromptMessages messages = builder.build(command);

    assertThat(messages.userMessage())
        .contains("FOLLOW_UP")
        .contains("desired_persona=calm_confident")
        .contains("critical_moment");
  }
}
