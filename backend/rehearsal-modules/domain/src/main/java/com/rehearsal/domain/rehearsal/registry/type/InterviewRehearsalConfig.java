package com.rehearsal.domain.rehearsal.registry.type;

import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.situation.model.SituationType;

public final class InterviewRehearsalConfig {

  private InterviewRehearsalConfig() {}

  public static RehearsalConfigDefinition definition() {
    return new RehearsalConfigDefinition(
        SituationType.INTERVIEW,
        3,
        "반갑습니다. 먼저 간단하게 자기소개 부탁드립니다.",
        "가장 자신 있게 설명할 수 있는 경험 하나를 말씀해주시겠어요?");
  }
}
