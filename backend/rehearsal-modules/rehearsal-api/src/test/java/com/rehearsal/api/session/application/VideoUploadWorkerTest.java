package com.rehearsal.api.session.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.support.InMemorySessionCache;
import com.rehearsal.api.support.TestClientSessions;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.session.model.VideoUploadStatus;
import com.rehearsal.domain.session.port.VideoStoragePort;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VideoUploadWorkerTest {

  @Test
  void completesSessionUploadStatusOnStorageSuccess() throws Exception {
    ClientSession session = TestClientSessions.sessionWith(SessionStatus.REHEARSAL_PLAYING);
    session.assignVideoUrl("http://localhost/mock-videos/test-session-id.webm");
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    Path tempFile = Files.createTempFile("video-upload-test-", ".tmp");
    Files.write(tempFile, "video-bytes".getBytes());
    VideoUploadWorker worker = workerWith(sessionCache, false);

    worker.uploadAsync(session.getSessionId(), tempFile, "recording.webm", "video/webm");

    ClientSession saved = sessionCache.findById(session.getSessionId()).orElseThrow();
    assertThat(saved.getVideoUploadStatus()).isEqualTo(VideoUploadStatus.COMPLETED);
    assertThat(Files.exists(tempFile)).isFalse();
  }

  @Test
  void marksSessionFailedWhenStorageUploadThrows() throws Exception {
    ClientSession session = TestClientSessions.sessionWith(SessionStatus.REHEARSAL_PLAYING);
    session.assignVideoUrl("http://localhost/mock-videos/test-session-id.webm");
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    Path tempFile = Files.createTempFile("video-upload-test-", ".tmp");
    Files.write(tempFile, "video-bytes".getBytes());
    VideoUploadWorker worker = workerWith(sessionCache, true);

    worker.uploadAsync(session.getSessionId(), tempFile, "recording.webm", "video/webm");

    ClientSession saved = sessionCache.findById(session.getSessionId()).orElseThrow();
    assertThat(saved.getVideoUploadStatus()).isEqualTo(VideoUploadStatus.FAILED);
    assertThat(saved.getVideoUploadFailureReason()).isEqualTo("disk full");
    assertThat(Files.exists(tempFile)).isFalse();
  }

  private VideoUploadWorker workerWith(InMemorySessionCache sessionCache, boolean shouldFail) {
    SessionReader sessionReader = new SessionReader(sessionCache);
    VideoStoragePort videoStoragePort =
        new VideoStoragePort() {
          @Override
          public String resolvePublicUrl(String sessionId, String originalFilename) {
            throw new UnsupportedOperationException();
          }

          @Override
          public void upload(
              String sessionId, InputStream content, String originalFilename, String contentType) {
            if (shouldFail) {
              throw new IllegalStateException("disk full");
            }
          }
        };
    return new VideoUploadWorker(sessionReader, sessionCache, videoStoragePort);
  }
}
