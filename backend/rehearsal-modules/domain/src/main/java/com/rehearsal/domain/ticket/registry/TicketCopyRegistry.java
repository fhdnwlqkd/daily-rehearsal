package com.rehearsal.domain.ticket.registry;

import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.ticket.model.ChangeCard;
import java.util.Map;
import java.util.Optional;

public final class TicketCopyRegistry {

  private static final Map<SituationType, TicketCopyDefinition> DEFINITIONS =
      Map.of(
          SituationType.DATE, date(),
          SituationType.INTERVIEW, interview(),
          SituationType.FIRST_DAY, firstDay());

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

  private static TicketCopyDefinition interview() {
    return new TicketCopyDefinition(
        SituationType.INTERVIEW,
        new ChangeCard(
            "답변의 핵심을 먼저 말하고 구체적인 경험으로 뒷받침하기",
            "질문의 의도를 끝까지 듣고 침착하게 답하기",
            "답변이 바로 떠오르지 않으면 잠시 정리한 뒤 핵심 경험부터 말하기"));
  }

  private static TicketCopyDefinition firstDay() {
    return new TicketCopyDefinition(
        SituationType.FIRST_DAY,
        new ChangeCard(
            "먼저 밝게 인사하고 맡은 역할이나 기대를 짧게 소개하기",
            "모르는 것은 솔직하게 질문하고 협업적인 태도 유지하기",
            "예상하지 못한 질문을 받으면 미소로 여유를 만든 뒤 정중하게 답하기"));
  }
}
