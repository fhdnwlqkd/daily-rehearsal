package com.rehearsal.domain.rehearsal.registry;

import com.rehearsal.domain.rehearsal.registry.type.DateRehearsalConfig;
import com.rehearsal.domain.rehearsal.registry.type.FirstDayRehearsalConfig;
import com.rehearsal.domain.rehearsal.registry.type.InterviewRehearsalConfig;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.Map;
import java.util.Optional;

public final class RehearsalConfigRegistry {

  private static final Map<SituationType, RehearsalConfigDefinition> DEFINITIONS =
      Map.of(
          SituationType.DATE, DateRehearsalConfig.definition(),
          SituationType.INTERVIEW, InterviewRehearsalConfig.definition(),
          SituationType.FIRST_DAY, FirstDayRehearsalConfig.definition());

  static {
    // 새 SituationType이 추가되고 여기 정의를 깜빡하면 기동 시점에 바로 실패하도록 강제한다.
    for (SituationType situationType : SituationType.values()) {
      if (!DEFINITIONS.containsKey(situationType)) {
        throw new IllegalStateException(
            "Missing RehearsalConfigDefinition for situation type: " + situationType);
      }
    }
  }

  private RehearsalConfigRegistry() {}

  public static Optional<RehearsalConfigDefinition> findByType(SituationType situationType) {
    return Optional.ofNullable(DEFINITIONS.get(situationType));
  }
}
