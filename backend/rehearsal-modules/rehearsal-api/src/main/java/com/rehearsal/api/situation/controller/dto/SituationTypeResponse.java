package com.rehearsal.api.situation.controller.dto;

import com.rehearsal.domain.situation.registry.SituationTypeDefinition;
import java.util.List;

public record SituationTypeResponse(
    String key, String label, int gestureOrder, String briefingTitle, List<String> exampleAnswers) {

  public SituationTypeResponse {
    exampleAnswers = exampleAnswers == null ? List.of() : List.copyOf(exampleAnswers);
  }

  public static SituationTypeResponse from(SituationTypeDefinition definition) {
    return new SituationTypeResponse(
        definition.key(),
        definition.label(),
        definition.gestureOrder(),
        definition.briefingTitle(),
        definition.exampleAnswers());
  }
}
