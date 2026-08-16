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
          new SchemaItemDef(ContextSlotType.SITUATION_DETAIL, RequiredLevel.REQUIRED, 10),
          new SchemaItemDef(ContextSlotType.DESIRED_PERSONA, RequiredLevel.REQUIRED, 20),
          new SchemaItemDef(ContextSlotType.DESIRED_OUTCOME, RequiredLevel.REQUIRED, 30),
          new SchemaItemDef(ContextSlotType.CONVERSATION_MATERIAL, RequiredLevel.REQUIRED, 40),
          new SchemaItemDef(ContextSlotType.CRITICAL_MOMENT, RequiredLevel.SOFT_REQUIRED, 50),
          new SchemaItemDef(ContextSlotType.COUNTERPART_CONTEXT, RequiredLevel.SOFT_REQUIRED, 60),
          new SchemaItemDef(ContextSlotType.RESPONSE_STYLE, RequiredLevel.SOFT_REQUIRED, 70),
          new SchemaItemDef(ContextSlotType.FAMILIARITY_LEVEL, RequiredLevel.SOFT_REQUIRED, 80),
          new SchemaItemDef(ContextSlotType.USER_STRENGTH, RequiredLevel.SOFT_REQUIRED, 90),
          new SchemaItemDef(ContextSlotType.PRIOR_INTERACTION_CONTEXT, RequiredLevel.OPTIONAL, 100),
          new SchemaItemDef(ContextSlotType.INTERACTION_SETTING, RequiredLevel.OPTIONAL, 110),
          new SchemaItemDef(ContextSlotType.SUPPORTING_EXAMPLE, RequiredLevel.OPTIONAL, 120),
          new SchemaItemDef(ContextSlotType.ANTICIPATED_QUESTION, RequiredLevel.OPTIONAL, 130),
          new SchemaItemDef(ContextSlotType.INTERACTION_CONSTRAINT, RequiredLevel.OPTIONAL, 140),
          new SchemaItemDef(ContextSlotType.OUTFIT_DIRECTION, RequiredLevel.OPTIONAL, 150))),
  INTERVIEW(
      SituationType.INTERVIEW,
      "면접 컨텍스트 스키마",
      1,
      List.of(
          new SchemaItemDef(ContextSlotType.SITUATION_DETAIL, RequiredLevel.REQUIRED, 10),
          new SchemaItemDef(ContextSlotType.DESIRED_PERSONA, RequiredLevel.REQUIRED, 20),
          new SchemaItemDef(ContextSlotType.CRITICAL_MOMENT, RequiredLevel.REQUIRED, 30),
          new SchemaItemDef(ContextSlotType.DESIRED_OUTCOME, RequiredLevel.SOFT_REQUIRED, 40),
          new SchemaItemDef(ContextSlotType.CONVERSATION_MATERIAL, RequiredLevel.SOFT_REQUIRED, 50),
          new SchemaItemDef(ContextSlotType.COUNTERPART_CONTEXT, RequiredLevel.SOFT_REQUIRED, 60),
          new SchemaItemDef(ContextSlotType.USER_STRENGTH, RequiredLevel.SOFT_REQUIRED, 70),
          new SchemaItemDef(ContextSlotType.SUPPORTING_EXAMPLE, RequiredLevel.SOFT_REQUIRED, 80),
          new SchemaItemDef(ContextSlotType.ANTICIPATED_QUESTION, RequiredLevel.SOFT_REQUIRED, 90),
          new SchemaItemDef(ContextSlotType.RESPONSE_STYLE, RequiredLevel.SOFT_REQUIRED, 100),
          new SchemaItemDef(ContextSlotType.FAMILIARITY_LEVEL, RequiredLevel.SOFT_REQUIRED, 110),
          new SchemaItemDef(ContextSlotType.INTERACTION_SETTING, RequiredLevel.OPTIONAL, 120),
          new SchemaItemDef(ContextSlotType.PRIOR_INTERACTION_CONTEXT, RequiredLevel.OPTIONAL, 130),
          new SchemaItemDef(ContextSlotType.INTERACTION_CONSTRAINT, RequiredLevel.OPTIONAL, 140),
          new SchemaItemDef(ContextSlotType.OUTFIT_DIRECTION, RequiredLevel.OPTIONAL, 150))),
  FIRST_DAY(
      SituationType.FIRST_DAY,
      "첫 출근 컨텍스트 스키마",
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
