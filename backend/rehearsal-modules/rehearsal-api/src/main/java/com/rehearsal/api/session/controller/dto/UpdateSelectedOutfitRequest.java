package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.session.usecase.command.UpdateSelectedOutfitCommand;
import jakarta.validation.constraints.NotBlank;

public record UpdateSelectedOutfitRequest(@NotBlank String selectedOutfitId) {

  public UpdateSelectedOutfitCommand toCommand(String sessionId) {
    return new UpdateSelectedOutfitCommand(sessionId, selectedOutfitId);
  }
}
