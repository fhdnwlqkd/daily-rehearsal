package com.rehearsal.domain.extraction;

import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.model.SlotType;
import java.util.List;

final class SlotExtractionTestFixtures {

  private SlotExtractionTestFixtures() {}

  static ContextSlotSchema p1Schema() {
    ContextSlotOption presentation = new ContextSlotOption(1L, "presentation", "발표");
    ContextSlotOption date = new ContextSlotOption(2L, "date", "소개팅/첫 만남");
    ContextSlotOption dailyReset = new ContextSlotOption(3L, "daily_reset", "일상 정돈");
    ContextSlot situationType =
        new ContextSlot(
            1L,
            "situation_type",
            "상황 유형",
            SlotType.SINGLE_SELECT,
            "내일 상황을 분류한다.",
            "내일 어떤 상황인지 알려주세요.",
            null,
            dailyReset,
            List.of(presentation, date, dailyReset));

    ContextSlotOption calmConfident = new ContextSlotOption(4L, "calm_confident", "차분하고 신뢰감 있는 모습");
    ContextSlot desiredPersona =
        new ContextSlot(
            2L,
            "desired_persona",
            "되고 싶은 모습",
            SlotType.SINGLE_SELECT,
            "보여주고 싶은 태도나 인상을 추출한다.",
            "내일 어떤 모습으로 보이고 싶은지 알려주세요.",
            null,
            calmConfident,
            List.of(calmConfident));

    ContextSlot criticalMoment =
        new ContextSlot(
            3L,
            "critical_moment",
            "결정적 순간",
            SlotType.TEXT,
            "가장 리허설하고 싶은 순간을 추출한다.",
            "가장 걱정되는 순간은 언제인가요?",
            "첫 반응을 말해야 하는 순간",
            null,
            List.of());

    ContextSlot anxietyPoint =
        new ContextSlot(
            4L,
            "anxiety_point",
            "걱정 포인트",
            SlotType.TEXT,
            "걱정하거나 불편한 지점을 추출한다.",
            "내일 가장 신경 쓰이는 점이 있나요?",
            "처음 시작이 어색할 수 있음",
            null,
            List.of());

    ContextSlot placeContext =
        new ContextSlot(
            5L,
            "place_context",
            "장소 맥락",
            SlotType.TEXT,
            "내일 가게 될 장소나 공간 분위기를 추출한다.",
            "어디에서 일어나는 상황인지 알려주세요.",
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
            new ContextSlotSchemaItem(2L, desiredPersona, RequiredLevel.REQUIRED, 20, true),
            new ContextSlotSchemaItem(3L, criticalMoment, RequiredLevel.REQUIRED, 30, true),
            new ContextSlotSchemaItem(4L, anxietyPoint, RequiredLevel.SOFT_REQUIRED, 40, true),
            new ContextSlotSchemaItem(5L, placeContext, RequiredLevel.OPTIONAL, 50, true)));
  }
}
