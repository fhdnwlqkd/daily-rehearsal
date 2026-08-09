package com.rehearsal.domain.ticket.registry;

import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.ticket.model.ChangeCard;

public record TicketCopyDefinition(SituationType situationType, ChangeCard fallbackChangeCard) {}
