package com.rehearsal.api.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.domain.core.annotation.Description;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Description("/api/** 요청에 X-API-KEY 헤더 검증 필터를 등록하는 설정")
@Configuration
@EnableConfigurationProperties(ApiKeyAuthProperties.class)
public class ApiKeyAuthConfiguration {

  @Bean
  public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilter(
      ApiKeyAuthProperties properties, ObjectMapper objectMapper) {
    FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new ApiKeyAuthFilter(properties, objectMapper));
    registration.addUrlPatterns("/api/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
