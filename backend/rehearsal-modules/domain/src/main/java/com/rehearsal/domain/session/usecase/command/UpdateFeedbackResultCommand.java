package com.rehearsal.domain.session.usecase.command;

import java.util.Map;

public record UpdateFeedbackResultCommand(String sessionId, Map<String, Object> feedbackResult) {}
