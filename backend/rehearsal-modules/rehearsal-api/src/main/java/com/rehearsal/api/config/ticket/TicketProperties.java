package com.rehearsal.api.config.ticket;

import com.rehearsal.domain.core.annotation.Description;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Description("yml에서 티켓 발급 설정을 바인딩하는 설정 모델")
@Getter
@Setter
@ConfigurationProperties(prefix = "rehearsal.ticket")
public class TicketProperties {

  @Description("영상 업로드가 완료되지 않았을 때 downloadUrl/qrPayload로 대신 사용할 정적 URL")
  private String downloadFallbackUrl = "http://localhost:8080/mock-videos/unavailable.webm";
}
