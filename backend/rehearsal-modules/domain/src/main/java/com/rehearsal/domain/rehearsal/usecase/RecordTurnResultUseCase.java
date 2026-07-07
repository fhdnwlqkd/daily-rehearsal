package com.rehearsal.domain.rehearsal.usecase;

public interface RecordTurnResultUseCase {

  void recordTurnResult(
      String sessionId, String userTranscript, boolean success, String feedback, boolean fallback);
}
