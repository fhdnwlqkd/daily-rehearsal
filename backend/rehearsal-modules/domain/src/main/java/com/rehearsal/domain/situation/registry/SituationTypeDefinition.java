package com.rehearsal.domain.situation.registry;

import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;

public record SituationTypeDefinition(
    SituationType situationType,
    String label,
    int gestureOrder,
    String briefingTitle,
    List<String> exampleAnswers) {

  public SituationTypeDefinition {
    exampleAnswers = exampleAnswers == null ? List.of() : List.copyOf(exampleAnswers);
  }

  public String key() {
    return situationType.key();
  }
}
