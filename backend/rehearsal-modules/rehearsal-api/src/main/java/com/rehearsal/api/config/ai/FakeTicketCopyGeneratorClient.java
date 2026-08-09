package com.rehearsal.api.config.ai;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.ticket.model.TicketCopyRawResult;
import com.rehearsal.domain.ticket.model.TicketGenerationCommand;
import com.rehearsal.domain.ticket.port.TicketCopyGeneratorClient;

@Description("외부 AI 호출 없이 local/test 환경에서 deterministic 티켓 카피를 만드는 fake generator")
public class FakeTicketCopyGeneratorClient implements TicketCopyGeneratorClient {

  @Override
  public TicketCopyRawResult generate(TicketGenerationCommand command) {
    return new TicketCopyRawResult("리허설 완료!", "오늘의 리허설을 성공적으로 마쳤어요. 실전에서도 잘 해내실 거예요.");
  }
}
