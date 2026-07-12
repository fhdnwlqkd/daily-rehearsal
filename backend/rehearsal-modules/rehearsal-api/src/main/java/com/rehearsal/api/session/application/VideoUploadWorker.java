package com.rehearsal.api.session.application;

import com.rehearsal.api.config.async.AsyncConfig;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.port.VideoStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Description("영상 실제 저장을 백그라운드 스레드에서 수행하고 결과를 세션 상태에 반영하는 워커")
@Component
@RequiredArgsConstructor
public class VideoUploadWorker {

  private static final Logger log = LoggerFactory.getLogger(VideoUploadWorker.class);

  private final SessionReader sessionReader;
  private final SessionCache sessionCache;
  private final VideoStoragePort videoStoragePort;

  @Async(AsyncConfig.VIDEO_UPLOAD_EXECUTOR)
  public void uploadAsync(
      String sessionId, Path tempFile, String originalFilename, String contentType) {
    try {
      try (InputStream content = Files.newInputStream(tempFile)) {
        videoStoragePort.upload(sessionId, content, originalFilename, contentType);
      }

      ClientSession session = sessionReader.get(sessionId);
      session.completeVideoUpload();
      sessionCache.save(session);
    } catch (Exception exception) {
      // 업로드 실패는 세션을 죽이는 사유가 아니라 상태(FAILED)로 흡수한다 — 이후 티켓 발급 쪽에서
      // "영상 없이 발급" 또는 "재업로드 안내"를 판단할 수 있도록.
      log.error("Video upload failed for session {}", sessionId, exception);
      ClientSession session = sessionReader.get(sessionId);
      session.failVideoUpload(exception.getMessage());
      sessionCache.save(session);
    } finally {
      deleteQuietly(tempFile);
    }
  }

  private void deleteQuietly(Path tempFile) {
    try {
      Files.deleteIfExists(tempFile);
    } catch (IOException exception) {
      log.warn("Failed to delete temp video file {}", tempFile, exception);
    }
  }
}
