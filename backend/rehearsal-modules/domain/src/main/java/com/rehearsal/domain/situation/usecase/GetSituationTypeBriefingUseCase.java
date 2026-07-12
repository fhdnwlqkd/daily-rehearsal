package com.rehearsal.domain.situation.usecase;

import com.rehearsal.domain.situation.registry.SituationTypeBriefingDefinition;

public interface GetSituationTypeBriefingUseCase {

  SituationTypeBriefingDefinition getSituationTypeBriefing(String situationType);
}
