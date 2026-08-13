package com.rehearsal.domain.session.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.situation.model.SituationType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SessionContextTest {

  @Test
  void valuesWithSituationTypeUsesCanonicalSituationType() {
    SessionContext context =
        SessionContext.from(
            SituationType.DATE,
            Map.of("situation_type", "interview", "desired_persona", "warm_natural"));

    assertThat(context.values()).doesNotContainKey("situation_type");
    assertThat(context.valuesWithSituationType())
        .containsEntry("situation_type", "date")
        .containsEntry("desired_persona", "warm_natural");
  }

  @Test
  void mergeKeepsCurrentValuesAndOverridesWithExtractedValues() {
    SessionContext context =
        SessionContext.from(
            SituationType.DATE,
            Map.of("desired_persona", "warm_natural", "critical_moment", "first greeting"));

    SessionContext merged =
        context.merge(
            Map.of("critical_moment", "payment timing", "relationship_context", "first date"));

    assertThat(merged.valuesWithSituationType())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("critical_moment", "payment timing")
        .containsEntry("relationship_context", "first date")
        .containsEntry("situation_type", "date");
  }

  @Test
  void mergeDoesNotEraseCurrentValuesWhenFollowUpExtractionReturnsNull() {
    SessionContext context =
        SessionContext.from(
            SituationType.DATE,
            Map.of("situation_detail", "친구 소개로 처음 만남", "desired_persona", "warm_natural"));
    Map<String, Object> followUpValues = new LinkedHashMap<>();
    followUpValues.put("situation_detail", null);
    followUpValues.put("desired_persona", null);
    followUpValues.put("desired_outcome", "서로 편하게 대화를 마치는 것");

    SessionContext merged = context.merge(followUpValues);

    assertThat(merged.values())
        .containsEntry("situation_detail", "친구 소개로 처음 만남")
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("desired_outcome", "서로 편하게 대화를 마치는 것");
  }
}
