package com.rehearsal.domain.extraction.service.utils;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Description("slot schema item을 active 여부와 priority 기준으로 다루기 위한 schema item 유틸리티")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlotSchemaItems {

  public static List<ContextSlotSchemaItem> activeItemsByPriority(ContextSlotSchema schema) {
    return schema.items().stream()
        .filter(ContextSlotSchemaItem::active)
        .sorted(Comparator.comparingInt(ContextSlotSchemaItem::priority))
        .toList();
  }
}
