package com.rehearsal.domain.ticket.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("티켓 발급 비동기 작업 상태. sessionId 단위로 저장되는 job 레코드")
public record TicketJob(
    String sessionId,
    TicketJobStatus status,
    @Description("PENDING/FAILED일 때 null") TicketPayload result,
    @Description("서버 로그/디버깅 전용. 클라이언트 응답에는 절대 노출하지 않는다. PENDING/COMPLETED일 때 null")
        String failureReason) {

  public static TicketJob pending(String sessionId) {
    return new TicketJob(sessionId, TicketJobStatus.PENDING, null, null);
  }

  public TicketJob complete(TicketPayload result) {
    return new TicketJob(sessionId, TicketJobStatus.COMPLETED, result, null);
  }

  public TicketJob fail(String failureReason) {
    return new TicketJob(sessionId, TicketJobStatus.FAILED, null, failureReason);
  }
}
