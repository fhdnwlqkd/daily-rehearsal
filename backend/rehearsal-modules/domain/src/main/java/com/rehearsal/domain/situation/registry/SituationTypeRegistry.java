package com.rehearsal.domain.situation.registry;

import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Optional;

public final class SituationTypeRegistry {

  private static final List<SituationTypeDefinition> DEFINITIONS =
      List.of(date(), businessMeeting());

  private static final List<SituationTypeBriefingDefinition> BRIEFINGS =
      List.of(dateBriefing(), businessMeetingBriefing());

  private SituationTypeRegistry() {}

  public static List<SituationTypeDefinition> findAll() {
    return DEFINITIONS;
  }

  public static Optional<SituationTypeDefinition> findByType(SituationType situationType) {
    return DEFINITIONS.stream()
        .filter(definition -> definition.situationType() == situationType)
        .findFirst();
  }

  public static Optional<SituationTypeDefinition> findByKey(String key) {
    return situationType(key).flatMap(SituationTypeRegistry::findByType);
  }

  public static Optional<SituationTypeBriefingDefinition> findBriefingByKey(String key) {
    return situationType(key).flatMap(SituationTypeRegistry::findBriefingByType);
  }

  public static Optional<SituationTypeBriefingDefinition> findBriefingByType(
      SituationType situationType) {
    return BRIEFINGS.stream()
        .filter(definition -> definition.situationType() == situationType)
        .findFirst();
  }

  private static Optional<SituationType> situationType(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    return SituationType.findByKey(key.strip());
  }

  private static SituationTypeDefinition date() {
    return new SituationTypeDefinition(SituationType.DATE, "소개팅");
  }

  private static SituationTypeDefinition businessMeeting() {
    return new SituationTypeDefinition(SituationType.BUSINESS_MEETING, "비즈니스 미팅");
  }

  private static SituationTypeBriefingDefinition dateBriefing() {
    return new SituationTypeBriefingDefinition(
        SituationType.DATE,
        "내일의 소개팅을 짧게 말해주세요",
        List.of("내일 소개팅이 있는데 첫 인사가 어색할까 봐 걱정돼요."));
  }

  private static SituationTypeBriefingDefinition businessMeetingBriefing() {
    return new SituationTypeBriefingDefinition(
        SituationType.BUSINESS_MEETING,
        "내일의 비즈니스 미팅을 짧게 말해주세요",
        List.of("내일 고객 미팅에서 핵심 내용을 차분하게 전달하고 싶어요."));
  }
}
