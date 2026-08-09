package com.rehearsal.domain.session.model;

import java.util.List;

public record ContextCollectionState(
    String sessionId,
    ContextStatus status,
    SessionContext context,
    List<String> missingSlotKeys,
    List<String> followUpQuestions) {

  public ContextCollectionState {
    missingSlotKeys = missingSlotKeys == null ? List.of() : List.copyOf(missingSlotKeys);
    followUpQuestions = followUpQuestions == null ? List.of() : List.copyOf(followUpQuestions);
  }
}
