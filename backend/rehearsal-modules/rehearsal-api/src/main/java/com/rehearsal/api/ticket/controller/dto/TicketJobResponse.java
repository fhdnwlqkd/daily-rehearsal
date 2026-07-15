package com.rehearsal.api.ticket.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.model.TicketJobStatus;
import com.rehearsal.domain.ticket.model.TicketPayload;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Description("티켓 발급 job 상태 응답. status에 따라 담기는 필드가 다르다")
public record TicketJobResponse(
    String sessionId,
    TicketJobStatus status,
    String title,
    String message,
    Boolean fallback,
    SituationType situationType,
    String selectedOutfitId,
    List<ConversationHistory> conversationHistory,
    List<TurnEvaluation> turnEvaluations,
    String videoUrl,
    Boolean videoAvailable,
    String downloadUrl,
    String qrPayload,
    String failureMessage) {

  private static final String FAILURE_MESSAGE = "다시 시도해보세요.";

  public static TicketJobResponse from(TicketJob job) {
    return switch (job.status()) {
      case PENDING -> pending(job);
      case COMPLETED -> completed(job);
      case FAILED -> failed(job);
    };
  }

  private static TicketJobResponse pending(TicketJob job) {
    return new TicketJobResponse(
        job.sessionId(),
        job.status(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static TicketJobResponse completed(TicketJob job) {
    TicketPayload result = job.result();
    return new TicketJobResponse(
        job.sessionId(),
        job.status(),
        result.title(),
        result.message(),
        result.fallback(),
        result.situationType(),
        result.selectedOutfitId(),
        result.conversationHistory(),
        result.turnEvaluations(),
        result.videoUrl(),
        result.videoAvailable(),
        result.downloadUrl(),
        result.qrPayload(),
        null);
  }

  private static TicketJobResponse failed(TicketJob job) {
    return new TicketJobResponse(
        job.sessionId(),
        job.status(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        FAILURE_MESSAGE);
  }
}
