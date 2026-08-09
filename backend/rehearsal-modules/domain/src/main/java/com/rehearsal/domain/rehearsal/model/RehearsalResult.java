package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("Final rehearsal video, ticket, and download result")
public record RehearsalResult(
    Long id, String sessionId, String videoUrl, String ticketSummary, String downloadUrl) {

  public static RehearsalResult create(
      String sessionId, String videoUrl, String ticketSummary, String downloadUrl) {
    return new RehearsalResult(null, sessionId, videoUrl, ticketSummary, downloadUrl);
  }
}
