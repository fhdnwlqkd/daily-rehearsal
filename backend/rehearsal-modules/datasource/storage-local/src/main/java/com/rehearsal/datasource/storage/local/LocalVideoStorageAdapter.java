package com.rehearsal.datasource.storage.local;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.port.VideoStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.RequiredArgsConstructor;

@Description("VideoStoragePort를 로컬 디스크에 구현한 mock adapter. 추후 S3 adapter로 교체될 자리")
@RequiredArgsConstructor
public class LocalVideoStorageAdapter implements VideoStoragePort {

  private static final String DEFAULT_EXTENSION = ".bin";

  private final String localRoot;
  private final String publicBaseUrl;

  @Override
  public String resolvePublicUrl(String sessionId, String originalFilename) {
    return trimTrailingSlash(publicBaseUrl) + "/" + sessionId + extensionOf(originalFilename);
  }

  @Override
  public void upload(
      String sessionId, InputStream content, String originalFilename, String contentType) {
    Path target = Path.of(localRoot, sessionId + extensionOf(originalFilename));
    try {
      Files.createDirectories(target.getParent());
      Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private String extensionOf(String originalFilename) {
    if (originalFilename == null) {
      return DEFAULT_EXTENSION;
    }
    int dotIndex = originalFilename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
      return DEFAULT_EXTENSION;
    }
    return originalFilename.substring(dotIndex);
  }

  private String trimTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
