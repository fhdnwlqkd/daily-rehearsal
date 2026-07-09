package com.rehearsal.domain.extraction.usecase;

import com.rehearsal.domain.extraction.model.ContextExtractionJob;

public interface SubmitContextExtractionUseCase {

  ContextExtractionJob submitBriefing(String sessionId, String transcript);

  ContextExtractionJob submitFollowUp(String sessionId, String transcript);
}
