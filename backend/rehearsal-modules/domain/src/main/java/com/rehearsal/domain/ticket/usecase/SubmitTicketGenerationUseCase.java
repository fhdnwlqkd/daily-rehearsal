package com.rehearsal.domain.ticket.usecase;

import com.rehearsal.domain.ticket.model.TicketJob;

public interface SubmitTicketGenerationUseCase {

  TicketJob submit(String sessionId);
}
