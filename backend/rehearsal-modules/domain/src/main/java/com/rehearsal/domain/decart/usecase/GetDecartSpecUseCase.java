package com.rehearsal.domain.decart.usecase;

import com.rehearsal.domain.decart.usecase.result.DecartSpecResult;

public interface GetDecartSpecUseCase {

  DecartSpecResult getDecartSpec(String sessionId, String outfitId);
}
