package com.rehearsal.domain.rehearsal.model;

public record SimulationStart(
    String sessionId, int currentTurn, int maxTurn, String opponentLine) {}
