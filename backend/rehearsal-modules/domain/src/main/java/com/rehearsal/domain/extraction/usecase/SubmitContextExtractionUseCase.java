package com.rehearsal.domain.extraction.usecase;

import com.rehearsal.domain.extraction.model.ContextExtractionJob;

public interface SubmitContextExtractionUseCase {

  ContextExtractionJob submitBriefingExtraction(String sessionId, String transcript);

  ContextExtractionJob submitFollowUpExtraction(String sessionId, String transcript);
}
