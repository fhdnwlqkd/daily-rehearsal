package com.rehearsal.domain.ticket.port;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.ticket.model.TicketJob;
import java.util.Optional;

@Description("티켓 발급 비동기 job 상태를 저장/조회하는 port")
public interface TicketJobStore {

  void save(TicketJob job);

  Optional<TicketJob> findById(String sessionId);
}
