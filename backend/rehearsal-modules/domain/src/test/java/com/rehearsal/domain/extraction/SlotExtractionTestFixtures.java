package com.rehearsal.domain.extraction;

import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import java.util.LinkedHashMap;
import java.util.Map;

final class SlotExtractionTestFixtures {

  private SlotExtractionTestFixtures() {}

  static ContextSlotSchemaType p1Schema() {
    return ContextSlotSchemaType.DATE;
  }

  static Map<String, Object> dateRequiredSlots() {
    Map<String, Object> slots = new LinkedHashMap<>();
    slots.put("situation_detail", "지인 소개로 처음 만나는 소개팅");
    slots.put("desired_persona", "warm_natural");
    slots.put("desired_outcome", "서로 편하게 대화를 마치는 것");
    slots.put("conversation_material", "전시와 산책");
    return slots;
  }
}
