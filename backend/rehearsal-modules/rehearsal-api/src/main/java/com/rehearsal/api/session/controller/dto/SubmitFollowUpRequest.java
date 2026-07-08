package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import jakarta.validation.constraints.NotBlank;

@Description("Request to submit user follow-up transcript")
public record SubmitFollowUpRequest(
    @Description("User follow-up transcript converted by frontend STT") @NotBlank
        String transcript) {}
