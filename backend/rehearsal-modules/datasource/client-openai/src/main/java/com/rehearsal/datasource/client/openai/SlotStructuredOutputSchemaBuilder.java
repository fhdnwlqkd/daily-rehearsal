package com.rehearsal.datasource.client.openai;

import com.openai.core.JsonValue;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.responses.ResponseFormatTextJsonSchemaConfig;
import com.rehearsal.datasource.client.openai.prompt.OpenAiPromptType;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.service.utils.SlotSchemaItems;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.SlotType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Description("slot schema를 OpenAI Structured Outputs에서 사용할 JSON Schema로 변환하는 client adapter 서비스")
public class SlotStructuredOutputSchemaBuilder {

  public ResponseFormatTextJsonSchemaConfig build(ContextSlotSchema schema) {
    return ResponseFormatTextJsonSchemaConfig.builder()
        .name(OpenAiPromptType.CONTEXT_SLOT_EXTRACTION.getPromptName())
        .description(OpenAiPromptType.CONTEXT_SLOT_EXTRACTION.getDescription())
        .schema(buildResponsesSchema(schema))
        .strict(true)
        .build();
  }

  public ResponseFormatTextJsonSchemaConfig.Schema buildResponsesSchema(ContextSlotSchema schema) {
    return ResponseFormatTextJsonSchemaConfig.Schema.builder()
        .additionalProperties(rootSchemaProperties(schema))
        .build();
  }

  public ResponseFormatJsonSchema buildChatResponseFormat(ContextSlotSchema schema) {
    return ResponseFormatJsonSchema.builder()
        .jsonSchema(
            ResponseFormatJsonSchema.JsonSchema.builder()
                .name(OpenAiPromptType.CONTEXT_SLOT_EXTRACTION.getPromptName())
                .description(OpenAiPromptType.CONTEXT_SLOT_EXTRACTION.getDescription())
                .schema(buildChatSchema(schema))
                .strict(true)
                .build())
        .build();
  }

  public ResponseFormatJsonSchema.JsonSchema.Schema buildChatSchema(ContextSlotSchema schema) {
    return ResponseFormatJsonSchema.JsonSchema.Schema.builder()
        .additionalProperties(rootSchemaProperties(schema))
        .build();
  }

  private Map<String, JsonValue> rootSchemaProperties(ContextSlotSchema schema) {
    Map<String, JsonValue> root = objectSchema();
    Map<String, JsonValue> rootProperties = new LinkedHashMap<>();

    Map<String, JsonValue> slots = objectSchema();
    Map<String, JsonValue> slotProperties = new LinkedHashMap<>();
    List<String> slotRequired = new ArrayList<>();

    for (ContextSlotSchemaItem item : SlotSchemaItems.activeItemsByPriority(schema)) {
      ContextSlot slot = item.slot();
      slotProperties.put(slot.slotKey(), json(slotValueSchema(slot)));
      slotRequired.add(slot.slotKey());
    }

    slots.put("properties", json(slotProperties));
    slots.put("required", json(slotRequired));

    rootProperties.put("slots", json(slots));

    root.put("properties", json(rootProperties));
    root.put("required", json(List.of("slots")));
    return root;
  }

  private Map<String, JsonValue> slotValueSchema(ContextSlot slot) {
    if (slot.slotType() == SlotType.SINGLE_SELECT) {
      return singleSelectSchema(slot);
    }

    return nullableStringSchema();
  }

  private Map<String, JsonValue> singleSelectSchema(ContextSlot slot) {
    Map<String, JsonValue> schema = nullableStringSchema();
    List<String> enumValues = new ArrayList<>();
    slot.options().stream().map(ContextSlotOption::optionKey).forEach(enumValues::add);
    enumValues.add(null);
    schema.put("enum", json(enumValues));
    return schema;
  }

  private Map<String, JsonValue> objectSchema() {
    Map<String, JsonValue> schema = new LinkedHashMap<>();
    schema.put("type", json("object"));
    schema.put("additionalProperties", json(false));
    return schema;
  }

  private Map<String, JsonValue> nullableStringSchema() {
    Map<String, JsonValue> schema = new LinkedHashMap<>();
    schema.put("type", json(List.of("string", "null")));
    return schema;
  }

  private JsonValue json(Object value) {
    return JsonValue.from(value);
  }
}
