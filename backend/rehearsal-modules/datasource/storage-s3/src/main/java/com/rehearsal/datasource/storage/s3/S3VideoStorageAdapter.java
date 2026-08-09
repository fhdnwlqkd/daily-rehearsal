package com.rehearsal.datasource.storage.s3;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.port.VideoStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Description("VideoStoragePort를 S3에 구현한 adapter. 객체는 {bucket}/videos/ prefix 아래에 저장된다.")
@RequiredArgsConstructor
public class S3VideoStorageAdapter implements VideoStoragePort {

  private static final String KEY_PREFIX = "videos/";
  private static final String DEFAULT_EXTENSION = ".bin";

  private final S3Client s3Client;
  private final String bucket;
  private final String publicBaseUrl;

  public static S3VideoStorageAdapter create(String bucket, String region, String publicBaseUrl) {
    S3Client s3Client = S3Client.builder().region(Region.of(region)).build();
    return new S3VideoStorageAdapter(s3Client, bucket, publicBaseUrl);
  }

  @Override
  public String resolvePublicUrl(String sessionId, String originalFilename) {
    return trimTrailingSlash(publicBaseUrl) + "/" + sessionId + extensionOf(originalFilename);
  }

  @Override
  public void upload(
      String sessionId, InputStream content, String originalFilename, String contentType) {
    // RequestBody.fromInputStream requires a known content length upfront, which this port's
    // signature doesn't provide, so the (short rehearsal-clip-sized) stream is buffered in memory.
    byte[] bytes = readAllBytes(content);
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key(sessionId, originalFilename))
            .contentType(contentType)
            .build();
    s3Client.putObject(request, RequestBody.fromBytes(bytes));
  }

  private String key(String sessionId, String originalFilename) {
    return KEY_PREFIX + sessionId + extensionOf(originalFilename);
  }

  private byte[] readAllBytes(InputStream content) {
    try {
      return content.readAllBytes();
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
