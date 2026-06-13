package com.rehearsal.datasource.client.gemini;

import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.model.SlotType;
import java.util.List;

public final class GeminiSlotTestFixtures {

  private GeminiSlotTestFixtures() {}

  public static ContextSlotSchema p1Schema() {
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
