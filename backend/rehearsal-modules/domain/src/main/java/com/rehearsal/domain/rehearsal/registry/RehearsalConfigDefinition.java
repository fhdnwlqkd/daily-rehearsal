package com.rehearsal.domain.rehearsal.registry;

import com.rehearsal.domain.situation.model.SituationType;

public record RehearsalConfigDefinition(
    SituationType situationType,
    int maxTurn,
    int maxAttemptsPerTurn,
    String firstOpponentLine,
    String nextLineFallback) {}
