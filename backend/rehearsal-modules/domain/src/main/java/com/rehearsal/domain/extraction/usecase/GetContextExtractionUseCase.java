package com.rehearsal.domain.extraction.usecase;

import com.rehearsal.domain.extraction.model.ContextExtractionJob;

public interface GetContextExtractionUseCase {

  ContextExtractionJob get(String sessionId, String jobId);
}
