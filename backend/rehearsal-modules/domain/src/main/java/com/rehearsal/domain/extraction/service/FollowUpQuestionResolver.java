package com.rehearsal.domain.extraction.service;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.service.utils.SlotSchemaItems;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType.SchemaItemDef;
import com.rehearsal.domain.slot.registry.ContextSlotType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Description("누락된 필수 slot과 follow-up 횟수에 따라 사용자에게 던질 보강 질문을 결정하는 서비스")
public class FollowUpQuestionResolver {

  private static final String DEFAULT_PREFIX = "내일의 장면이 거의 완성됐어요. 마지막으로 ";

  public String resolve(
      ContextSlotSchemaType schema, List<String> missingRequiredSlotKeys, int followUpAttempt) {
    if (missingRequiredSlotKeys == null || missingRequiredSlotKeys.isEmpty()) {
      return null;
    }

    if (followUpAttempt >= schema.getMaxFollowUpAttempt()) {
      return null;
    }

    String fallbackQuestion =
        SlotSchemaItems.activeItemsByPriority(schema).stream()
            .map(SchemaItemDef::slotType)
            .filter(slot -> missingRequiredSlotKeys.contains(slot.getKey()))
            .map(ContextSlotType::getFollowUpHint)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(hint -> !hint.isBlank())
            .collect(Collectors.joining(" "));

    if (fallbackQuestion.isBlank()) {
      return null;
    }

    return DEFAULT_PREFIX + fallbackQuestion;
  }
}
