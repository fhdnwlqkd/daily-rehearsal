package com.rehearsal.api.config.ticket;

import com.rehearsal.domain.core.annotation.Description;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Description("Configuration for mobile ticket download URLs")
@Getter
@Setter
@ConfigurationProperties(prefix = "rehearsal.ticket")
public class TicketProperties {

  private String downloadPageBaseUrl = "http://localhost:3000";
}
