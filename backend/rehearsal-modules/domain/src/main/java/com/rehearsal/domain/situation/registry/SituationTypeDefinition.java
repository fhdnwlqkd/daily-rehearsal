package com.rehearsal.domain.situation.registry;

import com.rehearsal.domain.situation.model.SituationType;

public record SituationTypeDefinition(SituationType situationType, String label) {

  public String key() {
    return situationType.key();
  }
}
