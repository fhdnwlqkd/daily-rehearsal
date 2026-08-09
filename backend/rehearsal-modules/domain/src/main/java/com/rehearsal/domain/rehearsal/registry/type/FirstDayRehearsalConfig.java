package com.rehearsal.domain.rehearsal.registry.type;

import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.situation.model.SituationType;

public final class FirstDayRehearsalConfig {

  private FirstDayRehearsalConfig() {}

  public static RehearsalConfigDefinition definition() {
    return new RehearsalConfigDefinition(
        SituationType.FIRST_DAY,
        3,
        "오늘부터 함께 일하게 됐다고 들었어요. 간단히 소개해주시겠어요?",
        "오늘 둘러보면서 궁금했던 점은 없었어요?");
  }
}
