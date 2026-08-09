package com.rehearsal.domain.ticket.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.situation.model.SituationType;
import org.junit.jupiter.api.Test;

class TicketCopyRegistryTest {

  @Test
  void findsChangeCardFallbackForDate() {
    TicketCopyDefinition definition =
        TicketCopyRegistry.findByType(SituationType.DATE).orElseThrow();

    assertThat(definition.situationType()).isEqualTo(SituationType.DATE);
    assertThat(definition.fallbackChangeCard().todayAction()).isNotBlank();
    assertThat(definition.fallbackChangeCard().tomorrowAttitude()).isNotBlank();
    assertThat(definition.fallbackChangeCard().ifThenPlan()).isNotBlank();
  }

  @Test
  void findsChangeCardFallbackForBusinessMeeting() {
    TicketCopyDefinition definition =
        TicketCopyRegistry.findByType(SituationType.BUSINESS_MEETING).orElseThrow();

    assertThat(definition.situationType()).isEqualTo(SituationType.BUSINESS_MEETING);
    assertThat(definition.fallbackChangeCard().todayAction()).isNotBlank();
    assertThat(definition.fallbackChangeCard().tomorrowAttitude()).isNotBlank();
    assertThat(definition.fallbackChangeCard().ifThenPlan()).isNotBlank();
  }
}
