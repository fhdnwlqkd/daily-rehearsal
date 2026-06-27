package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.core.annotation.Description;

@Description("Request to create a P1 client session")
public record CreateSessionRequest(String channel) {}
