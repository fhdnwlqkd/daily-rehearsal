package com.rehearsal.api.config.ai;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.ticket.model.ChangeCard;
import com.rehearsal.domain.ticket.model.TicketCopyRawResult;
import com.rehearsal.domain.ticket.model.TicketGenerationCommand;
import com.rehearsal.domain.ticket.port.TicketCopyGeneratorClient;

@Description("Creates deterministic change-card copy without an external AI call")
public class FakeTicketCopyGeneratorClient implements TicketCopyGeneratorClient {

  @Override
  public TicketCopyRawResult generate(TicketGenerationCommand command) {
    return new TicketCopyRawResult(
        new ChangeCard(
            "첫 문장을 천천히 또렷하게 시작하기", "상대의 반응을 여유 있게 듣고 이어가기", "긴장되면 숨을 고른 뒤 준비한 한 문장부터 말하기"));
  }
}
