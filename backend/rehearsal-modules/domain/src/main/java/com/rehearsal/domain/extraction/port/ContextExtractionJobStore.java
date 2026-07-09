package com.rehearsal.domain.extraction.port;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import java.util.Optional;

@Description("Stores and reads briefing/follow-up context extraction async jobs")
public interface ContextExtractionJobStore {

  void save(ContextExtractionJob job);

  Optional<ContextExtractionJob> findById(String sessionId, String jobId);
}
