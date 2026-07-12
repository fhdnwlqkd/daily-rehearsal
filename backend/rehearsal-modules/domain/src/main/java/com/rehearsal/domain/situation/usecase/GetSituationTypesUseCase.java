package com.rehearsal.domain.situation.usecase;

import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;

public interface GetSituationTypesUseCase {

  List<SituationType> getSituationTypes();
}
