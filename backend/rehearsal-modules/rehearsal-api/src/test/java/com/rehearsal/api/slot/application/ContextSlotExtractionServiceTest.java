package com.rehearsal.api.slot.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.slot.application.command.ExtractContextSlotsCommand;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import com.rehearsal.domain.extraction.service.SlotExtractionProcessor;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.model.SlotType;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextSlotExtractionServiceTest {

  @Test
  void extractsAndProcessesContextSlots() {
    GetContextSlotSchemaUseCase getContextSlotSchemaUseCase = command -> p1Schema();
    SlotExtractorClient slotExtractorClient =
        command ->
            new SlotExtractionRawResult(
                Map.of("situation_type", "presentation", "critical_moment", "첫 질문에 답하는 순간"));
    ContextSlotExtractionService service =
        new ContextSlotExtractionService(
            getContextSlotSchemaUseCase, slotExtractorClient, new SlotExtractionProcessor());

    ExtractContextSlotsResult result =
        service.extract(new ExtractContextSlotsCommand("내일 발표가 걱정돼."));

    assertThat(result.schemaKey()).isEqualTo("date");
    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.followUpQuestion()).isNull();
    assertThat(result.missingRequiredSlotKeys()).isEmpty();
    assertThat(result.context())
        .containsEntry("situation_type", "presentation")
        .containsEntry("critical_moment", "첫 질문에 답하는 순간");
  }

  private ContextSlotSchema p1Schema() {
    ContextSlotOption presentation = new ContextSlotOption(1L, "presentation", "발표");
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
            List.of(presentation));
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
        "date",
        "P1 Offline Default Context Slot Schema",
        1,
        true,
        List.of(
            new ContextSlotSchemaItem(1L, situationType, RequiredLevel.REQUIRED, 10, true),
            new ContextSlotSchemaItem(2L, criticalMoment, RequiredLevel.REQUIRED, 20, true)));
  }
}
