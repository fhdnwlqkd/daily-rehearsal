package com.rehearsal.api.situation.application;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.situation.usecase.GetSituationTypeBriefingUseCase;
import com.rehearsal.domain.situation.usecase.GetSituationTypesUseCase;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Description("Application service for static situation type options")
@Service
public class SituationTypeService
    implements GetSituationTypesUseCase, GetSituationTypeBriefingUseCase {

  @Override
  public List<SituationType> getSituationTypes() {
    return Arrays.asList(SituationType.values());
  }

  @Override
  public SituationType getSituationTypeBriefing(String situationType) {
    return SituationType.fromKey(situationType);
  }
}
