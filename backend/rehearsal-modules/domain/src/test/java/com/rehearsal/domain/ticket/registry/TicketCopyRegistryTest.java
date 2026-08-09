package com.rehearsal.domain.ticket.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.situation.model.SituationType;
import org.junit.jupiter.api.Test;

class TicketCopyRegistryTest {

  @Test
  void findsTicketCopyForDate() {
    TicketCopyDefinition definition =
        TicketCopyRegistry.findByType(SituationType.DATE).orElseThrow();

    assertThat(definition.situationType()).isEqualTo(SituationType.DATE);
    assertThat(definition.fallbackTitle()).isNotBlank();
    assertThat(definition.fallbackMessage()).isNotBlank();
  }

  @Test
  void findsTicketCopyForEverySupportedType() {
    for (SituationType situationType : SituationType.values()) {
      TicketCopyDefinition definition = TicketCopyRegistry.findByType(situationType).orElseThrow();

      assertThat(definition.situationType()).isEqualTo(situationType);
      assertThat(definition.fallbackTitle()).isNotBlank();
      assertThat(definition.fallbackMessage()).isNotBlank();
    }
  }
}
