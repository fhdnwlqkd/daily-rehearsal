package com.rehearsal.api.config.storage;

import com.rehearsal.datasource.storage.local.LocalVideoStorageAdapter;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.session.port.VideoStoragePort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Description("yml의 storage 설정에 따라 VideoStoragePort bean을 구성하는 Spring 설정")
@Configuration
@EnableConfigurationProperties(VideoStorageProperties.class)
public class VideoStorageConfiguration {

  @Bean
  public VideoStoragePort videoStoragePort(VideoStorageProperties properties) {
    return new LocalVideoStorageAdapter(properties.getLocalRoot(), properties.getPublicBaseUrl());
  }
}
