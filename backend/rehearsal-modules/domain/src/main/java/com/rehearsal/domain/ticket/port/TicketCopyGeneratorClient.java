package com.rehearsal.domain.ticket.port;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.ticket.model.TicketCopyRawResult;
import com.rehearsal.domain.ticket.model.TicketGenerationCommand;

@Description("티켓 제목/문구를 생성하는 외부 AI client port")
public interface TicketCopyGeneratorClient {

  TicketCopyRawResult generate(TicketGenerationCommand command);
}
