package com.rehearsal.api.ticket.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.ticket.model.ChangeCard;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.model.TicketJobStatus;
import com.rehearsal.domain.ticket.model.TicketPayload;
import com.rehearsal.domain.ticket.model.TicketSnapshot;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Description("Ticket generation job state and completed change-card result")
public record TicketJobResponse(
    String sessionId,
    TicketJobStatus status,
    TicketSnapshot snapshot,
    ChangeCard changeCard,
    Boolean fallback,
    String videoUrl,
    Boolean videoAvailable,
    String downloadUrl,
    String qrPayload,
    String failureMessage) {

  private static final String FAILURE_MESSAGE = "티켓을 다시 발급해 주세요.";

  public static TicketJobResponse from(TicketJob job) {
    return switch (job.status()) {
      case PENDING -> pending(job);
      case COMPLETED -> completed(job);
      case FAILED -> failed(job);
    };
  }

  private static TicketJobResponse pending(TicketJob job) {
    return new TicketJobResponse(
        job.sessionId(), job.status(), null, null, null, null, null, null, null, null);
  }

  private static TicketJobResponse completed(TicketJob job) {
    TicketPayload result = job.result();
    return new TicketJobResponse(
        job.sessionId(),
        job.status(),
        result.snapshot(),
        result.changeCard(),
        result.fallback(),
        result.videoUrl(),
        result.videoAvailable(),
        result.downloadUrl(),
        result.qrPayload(),
        null);
  }

  private static TicketJobResponse failed(TicketJob job) {
    return new TicketJobResponse(
        job.sessionId(), job.status(), null, null, null, null, null, null, null, FAILURE_MESSAGE);
  }
}
