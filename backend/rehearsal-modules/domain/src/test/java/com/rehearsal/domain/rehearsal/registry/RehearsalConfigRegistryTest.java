package com.rehearsal.domain.rehearsal.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.situation.model.SituationType;
import org.junit.jupiter.api.Test;

class RehearsalConfigRegistryTest {

  @Test
  void findsRehearsalConfigForDate() {
    RehearsalConfigDefinition definition =
        RehearsalConfigRegistry.findByType(SituationType.DATE).orElseThrow();

    assertThat(definition.situationType()).isEqualTo(SituationType.DATE);
    assertThat(definition.maxTurn()).isEqualTo(3);
    assertThat(definition.maxAttemptsPerTurn()).isEqualTo(2);
    assertThat(definition.firstOpponentLine()).isNotBlank();
    assertThat(definition.nextLineFallback()).isNotBlank();
  }

  @Test
  void findsRehearsalConfigForBusinessMeeting() {
    RehearsalConfigDefinition definition =
        RehearsalConfigRegistry.findByType(SituationType.BUSINESS_MEETING).orElseThrow();

    assertThat(definition.situationType()).isEqualTo(SituationType.BUSINESS_MEETING);
    assertThat(definition.maxTurn()).isEqualTo(3);
    assertThat(definition.maxAttemptsPerTurn()).isEqualTo(2);
    assertThat(definition.firstOpponentLine()).isNotBlank();
    assertThat(definition.nextLineFallback()).isNotBlank();
  }
}
