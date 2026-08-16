package com.rehearsal.api.session.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.VideoUploadStatus;

@Description("영상 업로드 접수 응답. videoUrl은 실제 업로드 완료 여부와 무관하게 즉시 확정된다.")
public record VideoUploadResponse(
    String sessionId, String videoUrl, VideoUploadStatus status, String failureReason) {

  public static VideoUploadResponse from(ClientSession session) {
    return new VideoUploadResponse(
        session.getSessionId(),
        session.getVideoUrl(),
        session.getVideoUploadStatus(),
        session.getVideoUploadFailureReason());
  }
}
