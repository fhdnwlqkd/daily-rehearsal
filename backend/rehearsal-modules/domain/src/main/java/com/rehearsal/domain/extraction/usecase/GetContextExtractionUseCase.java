package com.rehearsal.domain.extraction.usecase;

import com.rehearsal.domain.session.model.ContextCollectionState;

public interface GetContextExtractionUseCase {

  ContextCollectionState getContext(String sessionId);
}
