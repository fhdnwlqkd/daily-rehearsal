package com.rehearsal.domain.decart.usecase;

import com.rehearsal.domain.decart.model.OutfitCandidate;
import java.util.List;

public interface GetOutfitCandidatesUseCase {

  List<OutfitCandidate> getOutfitCandidates(String sessionId);
}
