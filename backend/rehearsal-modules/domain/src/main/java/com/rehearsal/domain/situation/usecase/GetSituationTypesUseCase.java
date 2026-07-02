package com.rehearsal.domain.situation.usecase;

import com.rehearsal.domain.situation.registry.SituationTypeDefinition;
import java.util.List;

public interface GetSituationTypesUseCase {

  List<SituationTypeDefinition> getSituationTypes();
}
