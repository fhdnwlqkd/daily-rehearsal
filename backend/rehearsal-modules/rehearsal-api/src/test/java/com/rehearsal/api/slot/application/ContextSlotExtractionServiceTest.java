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
                Map.of(
                    "situation_type", "presentation", "critical_moment", "first question moment"));
    ContextSlotExtractionService service =
        new ContextSlotExtractionService(
            getContextSlotSchemaUseCase, slotExtractorClient, new SlotExtractionProcessor());

    ExtractContextSlotsResult result =
        service.extract(new ExtractContextSlotsCommand("tomorrow presentation rehearsal"));

    assertThat(result.schemaKey()).isEqualTo("date");
    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.followUpQuestion()).isNull();
    assertThat(result.missingRequiredSlotKeys()).isEmpty();
    assertThat(result.context())
        .containsEntry("situation_type", "presentation")
        .containsEntry("critical_moment", "first question moment");
  }

  @Test
  void fallsBackToDefaultContextWhenExtractorFails() {
    GetContextSlotSchemaUseCase getContextSlotSchemaUseCase = command -> p1Schema();
    SlotExtractorClient slotExtractorClient =
        command -> {
          throw new IllegalStateException("AI unavailable");
        };
    ContextSlotExtractionService service =
        new ContextSlotExtractionService(
            getContextSlotSchemaUseCase, slotExtractorClient, new SlotExtractionProcessor());

    ExtractContextSlotsResult result =
        service.extract(new ExtractContextSlotsCommand("tomorrow presentation rehearsal"));

    assertThat(result.readyForSimulation()).isTrue();
    assertThat(result.followUpQuestion()).isNull();
    assertThat(result.context())
        .containsEntry("situation_type", "presentation")
        .containsEntry("critical_moment", "default critical moment");
  }

  private ContextSlotSchema p1Schema() {
    ContextSlotOption presentation = new ContextSlotOption(1L, "presentation", "Presentation");
    ContextSlot situationType =
        new ContextSlot(
            1L,
            "situation_type",
            "Situation type",
            SlotType.SINGLE_SELECT,
            "Classify tomorrow's situation.",
            "Which situation do you want to rehearse?",
            null,
            presentation,
            List.of(presentation));
    ContextSlot criticalMoment =
        new ContextSlot(
            2L,
            "critical_moment",
            "Critical moment",
            SlotType.TEXT,
            "Extract the moment the user wants to rehearse.",
            "Which moment are you most worried about?",
            "default critical moment",
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
