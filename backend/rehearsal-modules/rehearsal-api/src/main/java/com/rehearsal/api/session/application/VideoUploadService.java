package com.rehearsal.api.session.application;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.port.VideoStoragePort;
import com.rehearsal.domain.session.repository.SessionRepository;
import com.rehearsal.domain.session.usecase.UploadSessionVideoUseCase;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Description("영상 업로드 요청을 접수해 videoUrl을 즉시 확정하고 실제 저장은 워커에 위임하는 서비스")
@Service
@RequiredArgsConstructor
public class VideoUploadService implements UploadSessionVideoUseCase {

  private final SessionReader sessionReader;
  private final SessionRepository sessionRepository;
  private final VideoStoragePort videoStoragePort;
  private final VideoUploadWorker videoUploadWorker;

  @Override
  public ClientSession upload(
      String sessionId, InputStream content, String originalFilename, String contentType) {
    ClientSession session = sessionReader.get(sessionId);

    // 멀티파트 요청의 InputStream은 이 메서드가 반환되면 무효화되므로, 비동기 워커로 넘기기 전에
    // 여기서 임시 파일로 전부 복사해 둔다.
    Path tempFile = copyToTempFile(content);
    String videoUrl = videoStoragePort.resolvePublicUrl(sessionId, originalFilename);

    session.assignVideoUrl(videoUrl);
    sessionRepository.saveSession(session);

    videoUploadWorker.uploadAsync(sessionId, tempFile, originalFilename, contentType);

    return session;
  }

  private Path copyToTempFile(InputStream content) {
    try {
      Path tempFile = Files.createTempFile("video-upload-", ".tmp");
      Files.copy(content, tempFile, StandardCopyOption.REPLACE_EXISTING);
      return tempFile;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
