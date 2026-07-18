package com.rehearsal.domain.extraction.service.utils;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType.SchemaItemDef;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Description("slot schema item을 active 여부와 priority 기준으로 다루기 위한 schema item 유틸리티")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlotSchemaItems {

  public static List<SchemaItemDef> activeItemsByPriority(ContextSlotSchemaType schema) {
    return schema.getItems().stream()
        .sorted(Comparator.comparingInt(SchemaItemDef::priority))
        .toList();
  }
}
