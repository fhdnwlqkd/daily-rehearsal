package com.rehearsal.datasource.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3VideoStorageAdapterTest {

  private static final String BUCKET = "test-bucket";
  private static final String PUBLIC_BASE_URL = "https://cdn.example.com/videos";

  @Test
  void resolvePublicUrlIsDeterministicForSameSessionAndFilename() {
    S3VideoStorageAdapter adapter =
        new S3VideoStorageAdapter(mock(S3Client.class), BUCKET, PUBLIC_BASE_URL);

    String first = adapter.resolvePublicUrl("session-1", "recording.webm");
    String second = adapter.resolvePublicUrl("session-1", "recording.webm");

    assertThat(first).isEqualTo(second);
    assertThat(first).isEqualTo("https://cdn.example.com/videos/session-1.webm");
  }

  @Test
  void resolvePublicUrlFallsBackToDefaultExtensionWhenFilenameHasNone() {
    S3VideoStorageAdapter adapter =
        new S3VideoStorageAdapter(mock(S3Client.class), BUCKET, PUBLIC_BASE_URL);

    String url = adapter.resolvePublicUrl("session-2", "recording");

    assertThat(url).isEqualTo("https://cdn.example.com/videos/session-2.bin");
  }

  @Test
  void uploadPutsObjectUnderVideosPrefixWithBucketAndContentType() {
    S3Client s3Client = mock(S3Client.class);
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willReturn(PutObjectResponse.builder().build());
    S3VideoStorageAdapter adapter = new S3VideoStorageAdapter(s3Client, BUCKET, PUBLIC_BASE_URL);
    byte[] content = "video-bytes".getBytes();

    adapter.upload("session-3", new ByteArrayInputStream(content), "recording.webm", "video/webm");

    ArgumentCaptor<PutObjectRequest> requestCaptor =
        ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
    PutObjectRequest request = requestCaptor.getValue();
    assertThat(request.bucket()).isEqualTo(BUCKET);
    assertThat(request.key()).isEqualTo("videos/session-3.webm");
    assertThat(request.contentType()).isEqualTo("video/webm");
  }
}
