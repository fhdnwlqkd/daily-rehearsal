package com.rehearsal.api.session.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Description("Briefing transcript normalization result response")
public record SubmitBriefingResponse(
    String sessionId, Map<String, Object> finalContext, List<String> followUpQuestions) {

  public static SubmitBriefingResponse from(ClientSession session) {
    return new SubmitBriefingResponse(
        session.getSessionId(),
        contextValues(session.getFinalContext()),
        session.getFollowUpQuestions());
  }

  private static Map<String, Object> contextValues(SessionContext context) {
    return context == null ? null : context.valuesWithSituationType();
  }
}
