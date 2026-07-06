package com.rehearsal.domain.situation.model;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import java.util.Arrays;
import java.util.Optional;

public enum SituationType {
  DATE("date"),
  BUSINESS_MEETING("business_meeting");

  private final String key;

  SituationType(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  public static SituationType fromKey(String key) {
    return findByKey(key).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
  }

  public static Optional<SituationType> findByKey(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(values()).filter(type -> type.key.equals(key.strip())).findFirst();
  }
}
