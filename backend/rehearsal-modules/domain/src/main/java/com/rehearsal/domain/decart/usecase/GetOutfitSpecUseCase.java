package com.rehearsal.domain.decart.usecase;

import com.rehearsal.domain.decart.model.DecartSpec;

public interface GetOutfitSpecUseCase {

  DecartSpec getOutfitSpec(String sessionId, String outfitId);
}
