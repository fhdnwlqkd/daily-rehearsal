package com.rehearsal.domain.situation.registry;

import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;

public record SituationTypeBriefingDefinition(
    SituationType situationType, String briefingTitle, List<String> exampleAnswers) {

  public SituationTypeBriefingDefinition {
    exampleAnswers = exampleAnswers == null ? List.of() : List.copyOf(exampleAnswers);
  }

  public String key() {
    return situationType.key();
  }
}
