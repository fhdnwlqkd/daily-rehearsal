package com.rehearsal.api.decart.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.decart.usecase.result.DecartSpecResult;

@Description("Decart WebRTC 연결에 필요한 client token과 VTON 스펙 응답")
public record DecartSpecResponse(String clientToken, SpecDto spec) {

  public record SpecDto(String model, String prompt, String referenceImageUrl, boolean enhance) {}

  public static DecartSpecResponse from(DecartSpecResult result) {
    return new DecartSpecResponse(
        result.clientToken(),
        new SpecDto(
            result.spec().model(),
            result.spec().prompt(),
            result.spec().referenceImageUrl(),
            result.spec().enhance()));
  }
}
