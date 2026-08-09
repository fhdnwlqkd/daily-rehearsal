package com.rehearsal.domain.ticket.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("변화 카드 왼쪽에 표시할 리허설 상황 요약")
public record TicketSnapshot(
    String situationLabel,
    String criticalMoment,
    String desiredPersonaLabel,
    String selectedOutfitLabel) {}
