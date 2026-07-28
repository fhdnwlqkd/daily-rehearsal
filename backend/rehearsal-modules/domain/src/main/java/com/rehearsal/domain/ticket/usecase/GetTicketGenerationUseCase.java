package com.rehearsal.domain.ticket.usecase;

import com.rehearsal.domain.ticket.model.TicketJob;

public interface GetTicketGenerationUseCase {

  TicketJob get(String sessionId);
}
