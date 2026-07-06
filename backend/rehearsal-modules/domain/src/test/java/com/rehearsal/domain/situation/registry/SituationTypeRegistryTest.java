package com.rehearsal.domain.situation.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.situation.model.SituationType;
import org.junit.jupiter.api.Test;

class SituationTypeRegistryTest {

  @Test
  void findsAllSituationTypesByGestureOrder() {
    assertThat(SituationTypeRegistry.findAll())
        .extracting(SituationTypeDefinition::situationType)
        .containsExactly(SituationType.DATE, SituationType.BUSINESS_MEETING);
  }

  @Test
  void findsSituationTypeDefinitionByKey() {
    SituationTypeDefinition definition = SituationTypeRegistry.findByKey("date").orElseThrow();

    assertThat(definition.situationType()).isEqualTo(SituationType.DATE);
    assertThat(definition.key()).isEqualTo("date");
    assertThat(definition.label()).isEqualTo("\uC18C\uAC1C\uD305");
    assertThat(definition.exampleAnswers()).isNotEmpty();
  }

  @Test
  void returnsEmptyWhenKeyIsUnknown() {
    assertThat(SituationTypeRegistry.findByKey("unknown")).isEmpty();
  }
}
