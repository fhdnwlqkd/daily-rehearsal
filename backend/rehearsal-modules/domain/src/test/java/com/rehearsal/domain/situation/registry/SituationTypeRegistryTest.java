package com.rehearsal.domain.situation.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.situation.model.SituationType;
import org.junit.jupiter.api.Test;

class SituationTypeRegistryTest {

  @Test
  void findsAllSituationTypes() {
    assertThat(SituationTypeRegistry.findAll())
        .extracting(SituationTypeDefinition::situationType)
        .containsExactly(SituationType.DATE, SituationType.BUSINESS_MEETING);
  }

  @Test
  void findsSituationTypeDefinitionByKey() {
    SituationTypeDefinition definition = SituationTypeRegistry.findByKey("date").orElseThrow();

    assertThat(definition.situationType()).isEqualTo(SituationType.DATE);
    assertThat(definition.key()).isEqualTo("date");
    assertThat(definition.label()).isEqualTo("소개팅");
  }

  @Test
  void findsSituationTypeBriefingDefinitionByKey() {
    SituationTypeBriefingDefinition briefing =
        SituationTypeRegistry.findBriefingByKey("date").orElseThrow();

    assertThat(briefing.situationType()).isEqualTo(SituationType.DATE);
    assertThat(briefing.key()).isEqualTo("date");
    assertThat(briefing.briefingTitle()).isEqualTo("내일의 소개팅을 짧게 말해주세요");
    assertThat(briefing.exampleAnswers()).isNotEmpty();
  }

  @Test
  void returnsEmptyWhenKeyIsUnknown() {
    assertThat(SituationTypeRegistry.findByKey("unknown")).isEmpty();
  }
}
