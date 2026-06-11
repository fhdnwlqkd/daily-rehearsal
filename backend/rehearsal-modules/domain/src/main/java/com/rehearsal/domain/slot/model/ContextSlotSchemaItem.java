package com.rehearsal.domain.slot.model;

import com.rehearsal.domain.core.annotation.Description;

public record ContextSlotSchemaItem(
    @Description("Context slot schema item DB id") Long id,
    @Description("Schema에 연결된 slot 상세") ContextSlot slot,
    @Description("이 schema 안에서 해당 slot이 얼마나 필수적인지 나타내는 수준") RequiredLevel requiredLevel,
    @Description("부족한 slot 질문과 화면 표시의 우선순위") int priority,
    @Description("Schema 안에서 해당 slot 사용 여부") boolean active) {}
