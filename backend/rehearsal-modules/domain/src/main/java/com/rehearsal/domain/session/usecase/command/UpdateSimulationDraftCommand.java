package com.rehearsal.domain.session.usecase.command;

import java.util.Map;

public record UpdateSimulationDraftCommand(String sessionId, Map<String, Object> simulationDraft) {}
