package com.rehearsal.domain.slot.registry;

import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.slot.model.RequiredLevel;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContextSlotSchemaType {
  DATE(
      SituationType.DATE,
      "소개팅 상황",
      1,
      List.of(
          new SchemaItemDef(ContextSlotType.DESIRED_PERSONA, RequiredLevel.REQUIRED, 10),
          new SchemaItemDef(ContextSlotType.CRITICAL_MOMENT, RequiredLevel.REQUIRED, 20),
          new SchemaItemDef(ContextSlotType.OUTFIT_DIRECTION, RequiredLevel.OPTIONAL, 30))),
  BUSINESS_MEETING(
      SituationType.BUSINESS_MEETING,
      "비즈니스 미팅 컨텍스트 스키마",
      1,
      List.of(
          new SchemaItemDef(ContextSlotType.DESIRED_PERSONA, RequiredLevel.REQUIRED, 10),
          new SchemaItemDef(ContextSlotType.CRITICAL_MOMENT, RequiredLevel.REQUIRED, 20),
          new SchemaItemDef(ContextSlotType.OUTFIT_DIRECTION, RequiredLevel.OPTIONAL, 30)));

  private final SituationType situationType;
  private final String schemaName;
  private final int maxFollowUpAttempt;
  private final List<SchemaItemDef> items;

  public record SchemaItemDef(
      ContextSlotType slotType, RequiredLevel requiredLevel, int priority) {}

  public static Optional<ContextSlotSchemaType> findBySituationType(SituationType situationType) {
    if (situationType == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(schema -> schema.situationType == situationType)
        .findFirst();
  }
}
