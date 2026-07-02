package com.rehearsal.domain.situation.registry;

import com.rehearsal.domain.situation.model.SituationType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SituationTypeRegistry {

  private static final Map<SituationType, SituationTypeDefinition> DEFINITIONS =
      Map.of(
          SituationType.DATE, date(),
          SituationType.BUSINESS_MEETING, businessMeeting());

  private SituationTypeRegistry() {}

  public static List<SituationTypeDefinition> findAll() {
    return DEFINITIONS.values().stream()
        .sorted(Comparator.comparingInt(SituationTypeDefinition::gestureOrder))
        .toList();
  }

  public static Optional<SituationTypeDefinition> findByType(SituationType situationType) {
    return Optional.ofNullable(DEFINITIONS.get(situationType));
  }

  public static Optional<SituationTypeDefinition> findByKey(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    return SituationType.findByKey(key.strip()).flatMap(SituationTypeRegistry::findByType);
  }

  private static SituationTypeDefinition date() {
    return new SituationTypeDefinition(
        SituationType.DATE,
        "\uC18C\uAC1C\uD305",
        1,
        "\uB0B4\uC77C\uC758 \uC18C\uAC1C\uD305\uC744 \uC9E7\uAC8C \uB9D0\uD574\uC8FC\uC138\uC694",
        List.of(
            "\uB0B4\uC77C \uC18C\uAC1C\uD305\uC774 \uC788\uB294\uB370 \uCCAB \uC778\uC0AC\uAC00 \uC5B4\uC0C9\uD560\uAE4C \uBD10 \uAC71\uC815\uB3FC\uC694."));
  }

  private static SituationTypeDefinition businessMeeting() {
    return new SituationTypeDefinition(
        SituationType.BUSINESS_MEETING,
        "\uBE44\uC988\uB2C8\uC2A4 \uBBF8\uD305",
        2,
        "\uB0B4\uC77C\uC758 \uBE44\uC988\uB2C8\uC2A4 \uBBF8\uD305\uC744 \uC9E7\uAC8C \uB9D0\uD574\uC8FC\uC138\uC694",
        List.of(
            "\uB0B4\uC77C \uACE0\uAC1D \uBBF8\uD305\uC5D0\uC11C \uD575\uC2EC \uB0B4\uC6A9\uC744 \uCC28\uBD84\uD558\uAC8C \uC804\uB2EC\uD558\uACE0 \uC2F6\uC5B4\uC694."));
  }
}
