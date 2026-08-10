package com.rehearsal.domain.rehearsal.model;

public record TurnEvaluation(
    int turnNo, TurnEvaluationOutcome outcome, String feedback, boolean fallback) {}
