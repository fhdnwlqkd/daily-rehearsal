package com.rehearsal.datasource.client.gemini;

import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;

public final class GeminiSlotTestFixtures {

  private GeminiSlotTestFixtures() {}

  public static ContextSlotSchemaType p1Schema() {
    return ContextSlotSchemaType.DATE;
  }
}
