package com.rehearsal.api.config.decart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.datasource.client.decart.DecartHttpTokenClient;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.decart.port.DecartTokenPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Description("yml의 Decart 설정에 따라 DecartTokenPort bean을 구성하는 Spring 설정")
@Configuration
@EnableConfigurationProperties(DecartProperties.class)
public class DecartConfiguration {

  @Bean
  public DecartTokenPort decartTokenPort(DecartProperties properties) {
    return new DecartHttpTokenClient(
        properties.getApiKey(), properties.getTokenEndpoint(), new ObjectMapper());
  }
}
