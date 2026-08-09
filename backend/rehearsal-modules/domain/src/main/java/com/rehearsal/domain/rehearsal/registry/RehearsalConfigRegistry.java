package com.rehearsal.domain.rehearsal.registry;

import com.rehearsal.domain.situation.model.SituationType;
import java.util.Map;
import java.util.Optional;

public final class RehearsalConfigRegistry {

  // TODO: 기획 확정 전 임시값. 상황별로 다른 턴 수가 필요해지면 아래 정의별로 다른 값을 넣으면 됨.
  private static final int DEFAULT_MAX_TURN = 3;
  private static final int DEFAULT_MAX_ATTEMPTS_PER_TURN = 2;

  // TODO: nextLineFallback 문구도 기획 확정 전 임시값.

  private static final Map<SituationType, RehearsalConfigDefinition> DEFINITIONS =
      Map.of(
          SituationType.DATE, date(),
          SituationType.BUSINESS_MEETING, businessMeeting());

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

  private static RehearsalConfigDefinition date() {
    return new RehearsalConfigDefinition(
        SituationType.DATE,
        DEFAULT_MAX_TURN,
        DEFAULT_MAX_ATTEMPTS_PER_TURN,
        "안녕하세요, 만나서 반가워요! 오늘 어떻게 지내셨어요?",
        "음, 그렇군요. 조금 더 이야기해주실 수 있어요?");
  }

  private static RehearsalConfigDefinition businessMeeting() {
    return new RehearsalConfigDefinition(
        SituationType.BUSINESS_MEETING,
        DEFAULT_MAX_TURN,
        DEFAULT_MAX_ATTEMPTS_PER_TURN,
        "안녕하세요, 오늘 미팅에 참석해주셔서 감사합니다.",
        "네, 알겠습니다. 관련해서 조금 더 설명해주시겠어요?");
  }
}
