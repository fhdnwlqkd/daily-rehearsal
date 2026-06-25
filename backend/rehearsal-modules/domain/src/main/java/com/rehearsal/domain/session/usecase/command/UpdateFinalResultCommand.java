package com.rehearsal.domain.session.usecase.command;

import java.util.Map;

public record UpdateFinalResultCommand(String sessionId, Map<String, Object> finalResult) {}
