package com.rehearsal.api.situation.controller.dto;

import com.rehearsal.domain.situation.model.SituationType;

public record SituationTypeBriefingResponse(
    String situationType, String briefingTitle, String exampleAnswer) {

  public static SituationTypeBriefingResponse from(SituationType situation) {
    return new SituationTypeBriefingResponse(
        situation.getKey(), situation.getBriefingTitle(), situation.getExampleAnswer());
  }
}
