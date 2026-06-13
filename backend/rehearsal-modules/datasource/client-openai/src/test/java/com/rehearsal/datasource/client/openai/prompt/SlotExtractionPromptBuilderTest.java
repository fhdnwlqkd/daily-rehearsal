package com.rehearsal.datasource.client.openai.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.extraction.model.SlotExtractionMode;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.model.SlotType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SlotExtractionPromptBuilderTest {

  private final SlotExtractionPromptBuilder builder = new SlotExtractionPromptBuilder();

  @Test
  void buildsDeveloperAndUserMessagesFromRuntimeSlotSchema() {
    SlotExtractionPromptRequest request =
        new SlotExtractionPromptRequest(p1Schema(), "  내일 발표에서 예상 질문을 받을 때 차분하게 답하고 싶어.  ", 0);

    OpenAiPromptMessages messages = builder.build(request);

    assertThat(messages.promptType()).isEqualTo(OpenAiPromptType.CONTEXT_SLOT_EXTRACTION);
    assertThat(messages.developerMessage())
        .contains("Structured Outputs schema", "Do not invent", "SINGLE_SELECT")
        .contains("Do not create follow-up questions")
        .contains("In INITIAL mode")
        .contains("In FOLLOW_UP mode");
    assertThat(messages.userMessage())
        .contains("TRANSCRIPT:")
        .contains("내일 발표에서 예상 질문을 받을 때 차분하게 답하고 싶어.")
        .contains("followUpAttempt=0")
        .contains("maxFollowUpAttempt=1")
        .contains("EXTRACTION_MODE:")
        .contains("INITIAL")
        .contains("slotKey: situation_type")
        .contains("requiredLevel: REQUIRED")
        .contains("options: [presentation, date]")
        .contains("slotKey: critical_moment")
        .contains("slotType: TEXT")
        .doesNotContain("followUpHint");
  }

  @Test
  void buildsFollowUpModeMessageWithCurrentSlotsAndTargets() {
    SlotExtractionPromptRequest request =
        new SlotExtractionPromptRequest(
            p1Schema(),
            "예상 질문을 받는 순간이 제일 걱정돼.",
            1,
            SlotExtractionMode.FOLLOW_UP,
            Map.of("situation_type", "presentation"),
            List.of("critical_moment"));

    OpenAiPromptMessages messages = builder.build(request);

    assertThat(messages.userMessage())
        .contains("FOLLOW_UP")
        .contains("CURRENT_SLOTS:")
        .contains("situation_type=presentation")
        .contains("TARGET_SLOT_KEYS:")
        .contains("critical_moment");
  }

  @Test
  void normalizesBlankTranscriptAndRejectsNegativeFollowUpAttempt() {
    SlotExtractionPromptRequest request = new SlotExtractionPromptRequest(p1Schema(), null, 0);

    assertThat(request.transcript()).isEmpty();
    assertThatThrownBy(() -> new SlotExtractionPromptRequest(p1Schema(), "", -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("followUpAttempt");
  }

  private ContextSlotSchema p1Schema() {
    ContextSlotOption presentation = new ContextSlotOption(1L, "presentation", "발표");
    ContextSlotOption date = new ContextSlotOption(2L, "date", "소개팅/첫 만남");
    ContextSlot situationType =
        new ContextSlot(
            1L,
            "situation_type",
            "상황 유형",
            SlotType.SINGLE_SELECT,
            "내일 상황을 분류한다.",
            "내일 어떤 상황인지 알려주세요.",
            null,
            presentation,
            List.of(presentation, date));
    ContextSlot criticalMoment =
        new ContextSlot(
            2L,
            "critical_moment",
            "결정적 순간",
            SlotType.TEXT,
            "가장 리허설하고 싶은 순간을 추출한다.",
            "가장 걱정되는 순간은 언제인가요?",
            null,
            null,
            List.of());

    return new ContextSlotSchema(
        1L,
        "p1_offline_default",
        "P1 Offline Default Context Slot Schema",
        1,
        true,
        List.of(
            new ContextSlotSchemaItem(1L, situationType, RequiredLevel.REQUIRED, 10, true),
            new ContextSlotSchemaItem(2L, criticalMoment, RequiredLevel.REQUIRED, 20, true)));
  }
}
