package com.rehearsal.api.situation.controller.dto;

import com.rehearsal.domain.situation.registry.SituationTypeDefinition;

public record SituationTypeResponse(String situationType, String label) {

  public static SituationTypeResponse from(SituationTypeDefinition definition) {
    return new SituationTypeResponse(definition.key(), definition.label());
  }
}
