package com.rehearsal.api.situation.controller.dto;

import com.rehearsal.domain.situation.model.SituationType;

public record SituationTypeResponse(String situationType, String label) {

  public static SituationTypeResponse from(SituationType situation) {
    return new SituationTypeResponse(situation.getKey(), situation.getDisplayName());
  }
}
