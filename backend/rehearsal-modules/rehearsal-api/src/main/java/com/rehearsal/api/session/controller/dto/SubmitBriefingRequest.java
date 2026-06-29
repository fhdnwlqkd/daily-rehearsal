package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import jakarta.validation.constraints.NotBlank;

@Description("Request to submit user briefing transcript")
public record SubmitBriefingRequest(
    @Description("User briefing transcript converted by frontend STT") @NotBlank
        String transcript) {}
