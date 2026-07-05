package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import jakarta.validation.constraints.NotBlank;

@Description("Request to confirm the final selected outfit for a session")
public record ConfirmOutfitRequest(
    @Description("Outfit id confirmed by the user") @NotBlank String selectedOutfitId) {}
