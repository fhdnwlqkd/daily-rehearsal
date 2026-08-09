package com.rehearsal.api.rehearsal.application;

import com.rehearsal.domain.rehearsal.model.TurnMetrics;

public record TurnEvaluationRequested(
    String sessionId, int turnNo, int attemptNo, TurnMetrics metrics) {}
