package com.rehearsal.api.config.slot;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.extraction.service.SlotExtractionProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Description("domain slot extraction service를 Spring bean으로 등록하는 설정")
@Configuration
public class SlotExtractionConfiguration {

  @Bean
  @ConditionalOnMissingBean(SlotExtractionProcessor.class)
  public SlotExtractionProcessor slotExtractionProcessor() {
    return new SlotExtractionProcessor();
  }
}
