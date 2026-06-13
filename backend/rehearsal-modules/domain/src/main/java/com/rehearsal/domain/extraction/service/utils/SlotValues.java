package com.rehearsal.domain.extraction.service.utils;

import com.rehearsal.domain.core.annotation.Description;
import java.lang.reflect.Array;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Description("slot 값의 공통 empty 판정 규칙을 제공하는 값 유틸리티")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlotValues {

  public static boolean isEmpty(Object value) {
    switch (value) {
      case null -> {
        return true;
      }
      case CharSequence charSequence -> {
        return charSequence.toString().isBlank();
      }
      case Collection<?> collection -> {
        return collection.isEmpty();
      }
      default -> {}
    }

    if (value.getClass().isArray()) {
      return Array.getLength(value) == 0;
    }

    return false;
  }
}
