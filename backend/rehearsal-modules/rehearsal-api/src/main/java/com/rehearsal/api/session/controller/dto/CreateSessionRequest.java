package com.rehearsal.api.session.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionRequest(@NotBlank String situationType) {}
