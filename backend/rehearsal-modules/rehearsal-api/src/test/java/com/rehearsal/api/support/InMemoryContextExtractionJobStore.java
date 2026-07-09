package com.rehearsal.api.support;

import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.port.ContextExtractionJobStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryContextExtractionJobStore implements ContextExtractionJobStore {

  private final Map<String, ContextExtractionJob> store = new HashMap<>();

  public InMemoryContextExtractionJobStore(ContextExtractionJob... jobs) {
    for (ContextExtractionJob job : jobs) {
      save(job);
    }
  }

  @Override
  public void save(ContextExtractionJob job) {
    store.put(key(job.sessionId(), job.jobId()), job);
  }

  @Override
  public Optional<ContextExtractionJob> findById(String sessionId, String jobId) {
    return Optional.ofNullable(store.get(key(sessionId, jobId)));
  }

  private String key(String sessionId, String jobId) {
    return sessionId + ":" + jobId;
  }
}
