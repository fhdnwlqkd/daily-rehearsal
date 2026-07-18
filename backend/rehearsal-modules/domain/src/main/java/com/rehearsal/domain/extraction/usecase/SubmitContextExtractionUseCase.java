package com.rehearsal.domain.extraction.usecase;

import com.rehearsal.domain.session.model.ClientSession;

public interface SubmitContextExtractionUseCase {

  ClientSession submitBriefingExtraction(String sessionId, String transcript);

  ClientSession submitFollowUpExtraction(String sessionId, String transcript);
}
