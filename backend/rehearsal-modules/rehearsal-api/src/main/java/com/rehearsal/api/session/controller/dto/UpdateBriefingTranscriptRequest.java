package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.session.usecase.command.UpdateBriefingTranscriptCommand;
import jakarta.validation.constraints.NotBlank;

public record UpdateBriefingTranscriptRequest(@NotBlank String briefingTranscript) {

  public UpdateBriefingTranscriptCommand toCommand(String sessionId) {
    return new UpdateBriefingTranscriptCommand(sessionId, briefingTranscript);
  }
}
