package com.rehearsal.domain.ticket.registry;

import com.rehearsal.domain.situation.model.SituationType;
import java.util.Map;
import java.util.Optional;

public final class TicketCopyRegistry {

  // TODO: 기획 확정 전 임시 문구.

  private static final Map<SituationType, TicketCopyDefinition> DEFINITIONS =
      Map.of(
          SituationType.DATE, date(),
          SituationType.BUSINESS_MEETING, businessMeeting());

  static {
    // 새 SituationType이 추가되고 여기 정의를 깜빡하면 기동 시점에 바로 실패하도록 강제한다.
    for (SituationType situationType : SituationType.values()) {
      if (!DEFINITIONS.containsKey(situationType)) {
        throw new IllegalStateException(
            "Missing TicketCopyDefinition for situation type: " + situationType);
      }
    }
  }

  private TicketCopyRegistry() {}

  public static Optional<TicketCopyDefinition> findByType(SituationType situationType) {
    return Optional.ofNullable(DEFINITIONS.get(situationType));
  }

  private static TicketCopyDefinition date() {
    return new TicketCopyDefinition(
        SituationType.DATE, "오늘의 데이트 리허설 완료!", "긴장했지만 잘 해내셨어요. 이 자신감 그대로 실전에서도 빛나길 바라요.");
  }

  private static TicketCopyDefinition businessMeeting() {
    return new TicketCopyDefinition(
        SituationType.BUSINESS_MEETING,
        "오늘의 미팅 리허설 완료!",
        "차분하게 잘 준비하셨어요. 실전 미팅에서도 이 흐름 그대로 가져가세요.");
  }
}
