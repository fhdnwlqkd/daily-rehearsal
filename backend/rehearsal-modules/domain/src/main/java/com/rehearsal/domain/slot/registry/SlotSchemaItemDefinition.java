package com.rehearsal.domain.slot.registry;

import com.rehearsal.domain.slot.model.RequiredLevel;

public record SlotSchemaItemDefinition(
    SlotDefinition slot, RequiredLevel requiredLevel, int priority) {}
