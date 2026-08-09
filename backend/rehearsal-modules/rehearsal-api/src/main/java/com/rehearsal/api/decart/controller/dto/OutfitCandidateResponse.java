package com.rehearsal.api.decart.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.decart.model.OutfitCandidate;

@Description("옷 후보 목록 조회 응답")
public record OutfitCandidateResponse(
    String outfitId, String label, String thumbnailUrl, boolean defaultOutfit) {

  public static OutfitCandidateResponse from(OutfitCandidate candidate) {
    return new OutfitCandidateResponse(
        candidate.outfitId(),
        candidate.label(),
        candidate.thumbnailUrl(),
        candidate.defaultOutfit());
  }
}
