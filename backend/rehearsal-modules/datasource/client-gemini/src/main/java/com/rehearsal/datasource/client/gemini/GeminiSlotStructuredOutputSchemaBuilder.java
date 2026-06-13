package com.rehearsal.datasource.client.gemini;

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

@Description(
    "slot schema를 Gemini responseJsonSchema에서 사용할 JSON Schema map으로 변환하는 client adapter 서비스")
public class GeminiSlotStructuredOutputSchemaBuilder {

  public Map<String, Object> build(ContextSlotSchema schema) {
    Map<String, Object> root = objectSchema();
    Map<String, Object> rootProperties = new LinkedHashMap<>();

    Map<String, Object> slots = objectSchema();
    Map<String, Object> slotProperties = new LinkedHashMap<>();
    List<String> slotRequired = new ArrayList<>();

    for (ContextSlotSchemaItem item : SlotSchemaItems.activeItemsByPriority(schema)) {
      ContextSlot slot = item.slot();
      slotProperties.put(slot.slotKey(), slotValueSchema(slot));
      slotRequired.add(slot.slotKey());
    }

    slots.put("properties", slotProperties);
    slots.put("required", slotRequired);

    rootProperties.put("slots", slots);
    root.put("properties", rootProperties);
    root.put("required", List.of("slots"));
    return root;
  }

  private Map<String, Object> slotValueSchema(ContextSlot slot) {
    if (slot.slotType() == SlotType.SINGLE_SELECT) {
      return singleSelectSchema(slot);
    }

    return nullableStringSchema();
  }

  private Map<String, Object> singleSelectSchema(ContextSlot slot) {
    Map<String, Object> schema = nullableStringSchema();
    List<String> enumValues = new ArrayList<>();
    slot.options().stream().map(ContextSlotOption::optionKey).forEach(enumValues::add);
    enumValues.add(null);
    schema.put("enum", enumValues);
    return schema;
  }

  private Map<String, Object> objectSchema() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("additionalProperties", false);
    return schema;
  }

  private Map<String, Object> nullableStringSchema() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", List.of("string", "null"));
    return schema;
  }
}
