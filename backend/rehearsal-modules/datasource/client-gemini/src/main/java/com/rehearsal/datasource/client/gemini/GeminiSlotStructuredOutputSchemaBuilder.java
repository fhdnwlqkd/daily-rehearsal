package com.rehearsal.datasource.client.gemini;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.service.utils.SlotSchemaItems;
import com.rehearsal.domain.slot.model.SlotType;
import com.rehearsal.domain.slot.registry.ContextSlotOptionType;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType.SchemaItemDef;
import com.rehearsal.domain.slot.registry.ContextSlotType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Description(
    "slot schema를 Gemini responseJsonSchema에서 사용할 JSON Schema map으로 변환하는 client adapter 서비스")
public class GeminiSlotStructuredOutputSchemaBuilder {

  public Map<String, Object> build(ContextSlotSchemaType schema) {
    Map<String, Object> root = objectSchema();
    Map<String, Object> rootProperties = new LinkedHashMap<>();

    Map<String, Object> slots = objectSchema();
    Map<String, Object> slotProperties = new LinkedHashMap<>();
    List<String> slotRequired = new ArrayList<>();

    for (SchemaItemDef item : SlotSchemaItems.activeItemsByPriority(schema)) {
      ContextSlotType slot = item.slotType();
      slotProperties.put(slot.getKey(), slotValueSchema(slot));
      slotRequired.add(slot.getKey());
    }

    slots.put("properties", slotProperties);
    slots.put("required", slotRequired);

    rootProperties.put("slots", slots);
    root.put("properties", rootProperties);
    root.put("required", List.of("slots"));
    return root;
  }

  private Map<String, Object> slotValueSchema(ContextSlotType slot) {
    if (slot.getSlotType() == SlotType.SINGLE_SELECT) {
      return singleSelectSchema(slot);
    }

    return nullableStringSchema();
  }

  private Map<String, Object> singleSelectSchema(ContextSlotType slot) {
    Map<String, Object> schema = nullableStringSchema();
    List<String> enumValues = new ArrayList<>();
    slot.getOptions().stream().map(ContextSlotOptionType::getKey).forEach(enumValues::add);
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
