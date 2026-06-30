package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.model.ClientSession;

@Description("Client session creation response")
public record CreateSessionResponse(String sessionId, String situationType) {

  public static CreateSessionResponse from(ClientSession session) {
    return new CreateSessionResponse(session.getSessionId(), session.getSituationType().key());
  }
}
