package com.rehearsal.api.situation.application;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.situation.registry.SituationTypeDefinition;
import com.rehearsal.domain.situation.registry.SituationTypeRegistry;
import com.rehearsal.domain.situation.usecase.GetSituationTypesUseCase;
import java.util.List;
import org.springframework.stereotype.Service;

@Description("Application service for static situation type options")
@Service
public class SituationTypeService implements GetSituationTypesUseCase {

  @Override
  public List<SituationTypeDefinition> getSituationTypes() {
    return SituationTypeRegistry.findAll();
  }
}
