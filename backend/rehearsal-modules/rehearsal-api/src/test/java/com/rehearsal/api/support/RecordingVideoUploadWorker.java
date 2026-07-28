package com.rehearsal.api.support;

import com.rehearsal.api.session.application.VideoUploadWorker;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link VideoUploadWorker}를 상속해 실제 uploadAsync 로직(임시파일 읽기, storage 호출)을 건너뛰고 호출 횟수만 기록하는 테스트 더블.
 * 서비스가 실제 업로드를 기다리지 않고 바로 반환한다는 것을 검증하는 데 사용한다.
 */
public class RecordingVideoUploadWorker extends VideoUploadWorker {

  private final AtomicInteger invocationCount = new AtomicInteger();
  private Path lastTempFile;

  public RecordingVideoUploadWorker() {
    super(null, null, null);
  }

  @Override
  public void uploadAsync(
      String sessionId, Path tempFile, String originalFilename, String contentType) {
    invocationCount.incrementAndGet();
    lastTempFile = tempFile;
  }

  public int invocationCount() {
    return invocationCount.get();
  }

  public Path lastTempFile() {
    return lastTempFile;
  }
}
