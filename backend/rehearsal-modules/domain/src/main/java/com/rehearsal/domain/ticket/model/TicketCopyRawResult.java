package com.rehearsal.domain.ticket.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("LLM 또는 fake generator가 반환한 원시 티켓 카피")
public record TicketCopyRawResult(ChangeCard changeCard) {}
