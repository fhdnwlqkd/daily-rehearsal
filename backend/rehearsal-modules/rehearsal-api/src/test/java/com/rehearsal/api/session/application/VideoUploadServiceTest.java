package com.rehearsal.api.session.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.support.InMemorySessionCache;
import com.rehearsal.api.support.RecordingVideoUploadWorker;
import com.rehearsal.api.support.TestClientSessions;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.session.model.VideoUploadStatus;
import com.rehearsal.domain.session.port.VideoStoragePort;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class VideoUploadServiceTest {

  @Test
  void assignsVideoUrlImmediatelyAndDispatchesAsyncUploadWithoutWaiting() {
    ClientSession session = TestClientSessions.sessionWith(SessionStatus.REHEARSAL_PLAYING);
    InMemorySessionCache sessionCache = new InMemorySessionCache(session);
    RecordingVideoUploadWorker worker = new RecordingVideoUploadWorker();
    VideoStoragePort videoStoragePort =
        new VideoStoragePort() {
          @Override
          public String resolvePublicUrl(String sessionId, String originalFilename) {
            return "http://localhost/mock-videos/" + sessionId + ".webm";
          }

          @Override
          public void upload(
              String sessionId, InputStream content, String originalFilename, String contentType) {
            throw new UnsupportedOperationException(
                "actual storage I/O must run in the async worker, not the request thread");
          }
        };
    VideoUploadService service =
        new VideoUploadService(
            new SessionReader(sessionCache), sessionCache, videoStoragePort, worker);

    ClientSession result =
        service.upload(
            session.getSessionId(),
            new ByteArrayInputStream("video-bytes".getBytes()),
            "recording.webm",
            "video/webm");

    assertThat(result.getVideoUrl())
        .isEqualTo("http://localhost/mock-videos/" + session.getSessionId() + ".webm");
    assertThat(result.getVideoUploadStatus()).isEqualTo(VideoUploadStatus.PENDING);
    assertThat(sessionCache.findById(session.getSessionId()).orElseThrow().getVideoUploadStatus())
        .isEqualTo(VideoUploadStatus.PENDING);
    assertThat(worker.invocationCount()).isEqualTo(1);
    assertThat(worker.lastTempFile()).isNotNull();
  }
}
