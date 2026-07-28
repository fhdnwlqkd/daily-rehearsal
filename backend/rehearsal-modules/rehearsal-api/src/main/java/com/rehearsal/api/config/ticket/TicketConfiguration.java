package com.rehearsal.api.config.ticket;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TicketProperties.class)
public class TicketConfiguration {}
