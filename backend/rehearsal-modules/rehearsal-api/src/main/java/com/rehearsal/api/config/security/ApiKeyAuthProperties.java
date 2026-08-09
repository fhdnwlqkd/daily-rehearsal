package com.rehearsal.api.config.security;

import com.rehearsal.domain.core.annotation.Description;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Description("/api/** 요청을 검증할 API Key 인증 설정")
@Getter
@Setter
@ConfigurationProperties(prefix = "rehearsal.security.api-key")
public class ApiKeyAuthProperties {

  private boolean enabled = true;
  private String key = "";
  private String headerName = "X-API-KEY";
}
