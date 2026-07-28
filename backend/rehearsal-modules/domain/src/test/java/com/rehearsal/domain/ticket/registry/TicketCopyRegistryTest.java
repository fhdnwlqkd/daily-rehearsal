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
  void findsTicketCopyForBusinessMeeting() {
    TicketCopyDefinition definition =
        TicketCopyRegistry.findByType(SituationType.BUSINESS_MEETING).orElseThrow();

    assertThat(definition.situationType()).isEqualTo(SituationType.BUSINESS_MEETING);
    assertThat(definition.fallbackTitle()).isNotBlank();
    assertThat(definition.fallbackMessage()).isNotBlank();
  }
}
