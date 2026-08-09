package com.rehearsal.domain.ticket.registry;

import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.ticket.model.ChangeCard;
import java.util.Map;
import java.util.Optional;

public final class TicketCopyRegistry {

  private static final Map<SituationType, TicketCopyDefinition> DEFINITIONS =
      Map.of(SituationType.DATE, date(), SituationType.BUSINESS_MEETING, businessMeeting());

  static {
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
        SituationType.DATE,
        new ChangeCard(
            "첫 인사를 천천히 또렷하게 시작하기",
            "상대의 반응을 여유 있게 듣고 자연스럽게 이어가기",
            "어색함이 느껴지면 가벼운 질문으로 대화를 다시 시작하기"));
  }

  private static TicketCopyDefinition businessMeeting() {
    return new TicketCopyDefinition(
        SituationType.BUSINESS_MEETING,
        new ChangeCard(
            "핵심 제안을 먼저 말하고 근거를 덧붙이기",
            "상대의 질문을 끝까지 듣고 차분하게 답하기",
            "질문이 바로 떠오르지 않으면 잠시 정리한 뒤 핵심부터 답하기"));
  }
}
