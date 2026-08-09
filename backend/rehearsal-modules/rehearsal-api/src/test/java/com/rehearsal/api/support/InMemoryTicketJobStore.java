package com.rehearsal.api.support;

import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.port.TicketJobStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryTicketJobStore implements TicketJobStore {

  private final Map<String, TicketJob> store = new HashMap<>();

  public InMemoryTicketJobStore(TicketJob... jobs) {
    for (TicketJob job : jobs) {
      save(job);
    }
  }

  @Override
  public void save(TicketJob job) {
    store.put(job.sessionId(), job);
  }

  @Override
  public Optional<TicketJob> findById(String sessionId) {
    return Optional.ofNullable(store.get(sessionId));
  }
}
