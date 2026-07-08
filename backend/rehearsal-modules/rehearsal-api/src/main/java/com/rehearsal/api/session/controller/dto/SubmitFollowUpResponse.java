package com.rehearsal.api.session.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.model.ClientSession;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Description("Follow-up transcript normalization result response")
public record SubmitFollowUpResponse(
    String sessionId, Map<String, Object> finalContext, List<String> followUpQuestions) {

  public static SubmitFollowUpResponse from(ClientSession session) {
    return new SubmitFollowUpResponse(
        session.getSessionId(), session.getFinalContext(), session.getFollowUpQuestions());
  }
}
