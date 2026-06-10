package com.rehearsal.domain.slot.model;

public record ContextSlotSchemaItem(
    Long id, ContextSlot slot, RequiredLevel requiredLevel, int priority, boolean active) {}
