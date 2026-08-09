package com.rehearsal.api.session.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextCollectionState;
import com.rehearsal.domain.session.model.ContextStatus;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ContextExtractionResponse(
    String sessionId,
    ContextStatus status,
    Map<String, Object> context,
    List<String> missingSlotKeys,
    List<String> followUpQuestions) {

  public static ContextExtractionResponse pending(ClientSession session) {
    return new ContextExtractionResponse(
        session.getSessionId(), session.getContextStatus(), null, List.of(), List.of());
  }

  public static ContextExtractionResponse from(ContextCollectionState state) {
    Map<String, Object> context =
        state.context() == null ? null : state.context().valuesWithSituationType();
    return new ContextExtractionResponse(
        state.sessionId(),
        state.status(),
        context,
        state.missingSlotKeys(),
        state.followUpQuestions());
  }
}
