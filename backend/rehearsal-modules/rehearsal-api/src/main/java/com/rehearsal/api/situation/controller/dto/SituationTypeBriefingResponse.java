package com.rehearsal.api.situation.controller.dto;

import com.rehearsal.domain.situation.registry.SituationTypeBriefingDefinition;
import java.util.List;

public record SituationTypeBriefingResponse(
    String situationType, String briefingTitle, List<String> exampleAnswers) {

  public SituationTypeBriefingResponse {
    exampleAnswers = exampleAnswers == null ? List.of() : List.copyOf(exampleAnswers);
  }

  public static SituationTypeBriefingResponse from(SituationTypeBriefingDefinition definition) {
    return new SituationTypeBriefingResponse(
        definition.key(), definition.briefingTitle(), definition.exampleAnswers());
  }
}
