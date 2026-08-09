package com.rehearsal.datasource.storage.local;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalVideoStorageAdapterTest {

  @TempDir Path tempDir;

  @Test
  void resolvePublicUrlIsDeterministicForSameSessionAndFilename() {
    LocalVideoStorageAdapter adapter =
        new LocalVideoStorageAdapter(tempDir.toString(), "http://localhost:8080/mock-videos");

    String first = adapter.resolvePublicUrl("session-1", "recording.webm");
    String second = adapter.resolvePublicUrl("session-1", "recording.webm");

    assertThat(first).isEqualTo(second);
    assertThat(first).isEqualTo("http://localhost:8080/mock-videos/session-1.webm");
  }

  @Test
  void resolvePublicUrlFallsBackToDefaultExtensionWhenFilenameHasNone() {
    LocalVideoStorageAdapter adapter =
        new LocalVideoStorageAdapter(tempDir.toString(), "http://localhost:8080/mock-videos");

    String url = adapter.resolvePublicUrl("session-2", "recording");

    assertThat(url).isEqualTo("http://localhost:8080/mock-videos/session-2.bin");
  }

  @Test
  void uploadWritesFileToLocalRoot() throws Exception {
    LocalVideoStorageAdapter adapter =
        new LocalVideoStorageAdapter(tempDir.toString(), "http://localhost:8080/mock-videos");
    byte[] content = "video-bytes".getBytes();

    adapter.upload("session-3", new ByteArrayInputStream(content), "recording.webm", "video/webm");

    Path savedFile = tempDir.resolve("session-3.webm");
    assertThat(Files.exists(savedFile)).isTrue();
    assertThat(Files.readAllBytes(savedFile)).isEqualTo(content);
  }
}
