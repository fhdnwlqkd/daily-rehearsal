package com.rehearsal.domain.session.usecase.command;

import java.util.Map;

public record CompleteSessionContextCommand(
    String sessionId, Map<String, Object> finalUserContext) {}
