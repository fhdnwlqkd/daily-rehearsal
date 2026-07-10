package com.rehearsal.domain.session.usecase;

import com.rehearsal.domain.session.model.ClientSession;

public interface UpdateClientSessionUseCase {

  ClientSession confirmOutfit(String sessionId, String selectedOutfitId);
}
