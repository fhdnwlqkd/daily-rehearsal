package com.rehearsal.domain.extraction.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.model.SlotType;

@Description("추출된 사용자 맥락을 slot 정의와 값 상태, 출처까지 포함해 표현하는 표준 값 모델")
public record ContextSlotValue(
    String slotKey,
    String label,
    SlotType slotType,
    RequiredLevel requiredLevel,
    int priority,
    Object value,
    ContextSlotValueStatus status,
    ContextSlotValueSource source) {}
