package com.rehearsal.domain.ticket.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("AI 실패 fallback 흡수까지 반영한, job 결과 조립에 사용하는 티켓 카피")
public record TicketCopyResult(String title, String message, boolean fallback) {}
