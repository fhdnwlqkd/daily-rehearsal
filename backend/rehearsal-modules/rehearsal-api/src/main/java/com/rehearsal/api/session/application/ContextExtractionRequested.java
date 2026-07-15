package com.rehearsal.api.session.application;

import com.rehearsal.domain.extraction.model.SlotExtractionMode;

public record ContextExtractionRequested(
    String sessionId, String transcript, SlotExtractionMode mode) {}
