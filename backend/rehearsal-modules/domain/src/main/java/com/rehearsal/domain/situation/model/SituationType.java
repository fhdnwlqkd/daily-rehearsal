package com.rehearsal.domain.situation.model;

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
    return findByKey(key)
        .orElseThrow(() -> new IllegalArgumentException("Unknown situation type: " + key));
  }

  public static Optional<SituationType> findByKey(String key) {
    return Arrays.stream(values()).filter(type -> type.key.equals(key)).findFirst();
  }
}
