package com.rehearsal.api.decart.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.decart.model.DecartSpec;

@Description("Decart VTON에 전달할 outfit 스펙 응답")
public record OutfitSpecResponse(
    String model, String prompt, String referenceImageUrl, boolean enhance) {

  public static OutfitSpecResponse from(DecartSpec spec) {
    return new OutfitSpecResponse(
        spec.model(), spec.prompt(), spec.referenceImageUrl(), spec.enhance());
  }
}
