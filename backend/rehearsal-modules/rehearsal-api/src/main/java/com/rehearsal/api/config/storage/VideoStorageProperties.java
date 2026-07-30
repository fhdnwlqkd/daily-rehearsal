package com.rehearsal.api.config.storage;

import com.rehearsal.domain.core.annotation.Description;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Description("yml에서 영상 storage 설정을 바인딩하는 설정 모델")
@Getter
@Setter
@ConfigurationProperties(prefix = "rehearsal.video-storage")
public class VideoStorageProperties {

  private Provider provider = Provider.LOCAL;
  private String localRoot = "./data/videos";
  private String publicBaseUrl = "http://localhost:8080/mock-videos";
  private S3 s3 = new S3();

  public enum Provider {
    LOCAL,
    S3
  }

  @Getter
  @Setter
  public static class S3 {

    private String bucket = "";
    private String region = "";
  }
}
