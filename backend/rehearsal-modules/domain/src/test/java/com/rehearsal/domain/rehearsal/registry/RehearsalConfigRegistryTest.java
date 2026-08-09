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
    assertThat(definition.firstTurn().opponentLine()).isNotBlank();
    assertThat(definition.technicalFallback().opponentLine()).isNotBlank();
  }

  @Test
  void findsRehearsalConfigForEverySupportedType() {
    for (SituationType situationType : SituationType.values()) {
      RehearsalConfigDefinition definition =
          RehearsalConfigRegistry.findByType(situationType).orElseThrow();

      assertThat(definition.situationType()).isEqualTo(situationType);
      assertThat(definition.maxTurn()).isEqualTo(3);
      assertThat(definition.firstTurn().sceneCue()).isNotBlank();
      assertThat(definition.firstTurn().actionPrompt()).isNotBlank();
      assertThat(definition.firstTurn().acceptedIntentHint()).isNotBlank();
      assertThat(definition.turnObjectives()).hasSize(3);
      assertThat(definition.technicalFallback().opponentLine()).isNotBlank();
    }
  }
}
