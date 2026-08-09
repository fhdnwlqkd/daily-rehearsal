package com.rehearsal.domain.slot.registry;

import com.rehearsal.domain.slot.model.SlotType;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContextSlotType {
  DESIRED_PERSONA(
      "desired_persona",
      "원하는 페르소나",
      SlotType.SINGLE_SELECT,
      "사용자가 내일 보여주고 싶은 페르소나를 정규화하세요.",
      "내일 어떤 인상을 남기고 싶나요?",
      null,
      ContextSlotOptionType.CALM_CONFIDENT,
      List.of(
          ContextSlotOptionType.CALM_CONFIDENT,
          ContextSlotOptionType.WARM_NATURAL,
          ContextSlotOptionType.SHARP_PREPARED)),
  CRITICAL_MOMENT(
      "critical_moment",
      "가장 걱정되는 순간",
      SlotType.TEXT,
      "사용자가 가장 걱정하거나 연습하고 싶은 순간을 추출하세요.",
      "가장 걱정되는 순간은 언제인가요?",
      "첫 인사와 가벼운 대화",
      null,
      List.of()),
  OUTFIT_DIRECTION(
      "outfit_direction",
      "의상 방향성",
      SlotType.SINGLE_SELECT,
      "사용자가 입어보고 싶은 의상 분위기를 정규화하세요.",
      "이 상황에 어떤 옷차림이 어울릴까요?",
      null,
      ContextSlotOptionType.NEAT_CASUAL,
      List.of(
          ContextSlotOptionType.NEAT_CASUAL,
          ContextSlotOptionType.FORMAL_CLEAN,
          ContextSlotOptionType.SOFT_FRIENDLY));

  private final String key;
  private final String label;
  private final SlotType slotType;
  private final String extractionHint;
  private final String followUpHint;
  private final String defaultLiteralValue;
  private final ContextSlotOptionType defaultOption;
  private final List<ContextSlotOptionType> options;
}
