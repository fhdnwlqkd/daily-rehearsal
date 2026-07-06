package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.model.ClientSession;

@Description("P1 client session identifier response")
public record SessionResponse(String sessionId) {

  public static SessionResponse from(ClientSession session) {
    return new SessionResponse(session.getSessionId());
  }
}
