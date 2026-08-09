package com.rehearsal.domain.rehearsal.registry.type;

import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.situation.model.SituationType;

public final class DateRehearsalConfig {

  private DateRehearsalConfig() {}

  public static RehearsalConfigDefinition definition() {
    return new RehearsalConfigDefinition(
        SituationType.DATE, 3, "안녕하세요. 기다리게 한 건 아니죠?", "오늘 여기까지 오는 길은 괜찮으셨어요?");
  }
}
