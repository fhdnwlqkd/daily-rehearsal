package com.rehearsal.api.slot.application;

import com.rehearsal.api.slot.application.command.ExtractContextSlotsCommand;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextSlotValue;
import com.rehearsal.domain.extraction.model.SlotExtractionCommand;
import com.rehearsal.domain.extraction.model.SlotExtractionProcessingResult;
import com.rehearsal.domain.extraction.model.SlotExtractionRawResult;
import com.rehearsal.domain.extraction.port.SlotExtractorClient;
import com.rehearsal.domain.extraction.service.SlotExtractionProcessor;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.command.GetContextSlotSchemaCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Description("active schema 조회, AI raw slot 추출, domain slot processing을 연결하는 application service")
@Service
@RequiredArgsConstructor
public class ContextSlotExtractionService {

  private final GetContextSlotSchemaUseCase getContextSlotSchemaUseCase;
  private final SlotExtractorClient slotExtractorClient;
  private final SlotExtractionProcessor slotExtractionProcessor;

  public ExtractContextSlotsResult extract(ExtractContextSlotsCommand command) {
    ContextSlotSchema schema =
        getContextSlotSchemaUseCase.getContextSlotSchema(
            new GetContextSlotSchemaCommand(command.schemaKey()));
    SlotExtractionRawResult rawResult =
        slotExtractorClient.extract(
            new SlotExtractionCommand(
                schema,
                command.transcript(),
                command.followUpAttempt(),
                command.mode(),
                command.currentSlots(),
                command.targetSlotKeys()));
    SlotExtractionProcessingResult processingResult =
        slotExtractionProcessor.process(schema, rawResult, command.followUpAttempt());

    return new ExtractContextSlotsResult(
        schema.schemaKey(),
        rawResult.rawSlots(),
        processingResult.slots(),
        context(processingResult.slots()),
        processingResult.missingRequiredSlotKeys(),
        processingResult.followUpQuestion(),
        processingResult.readyForSimulation());
  }

  private Map<String, Object> context(Map<String, ContextSlotValue> slots) {
    Map<String, Object> context = new LinkedHashMap<>();
    for (Map.Entry<String, ContextSlotValue> entry : slots.entrySet()) {
      context.put(entry.getKey(), entry.getValue().value());
    }
    return context;
  }
}
