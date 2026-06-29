package com.rehearsal.api.decart.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record DecartTokenRequest(@NotBlank String outfitId) {}
