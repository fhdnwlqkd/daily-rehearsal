package com.rehearsal.domain.ticket.registry;

import com.rehearsal.domain.situation.model.SituationType;

public record TicketCopyDefinition(
    SituationType situationType, String fallbackTitle, String fallbackMessage) {}
