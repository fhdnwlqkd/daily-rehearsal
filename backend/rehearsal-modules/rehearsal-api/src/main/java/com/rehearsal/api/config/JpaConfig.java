package com.rehearsal.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.rehearsal.datasource.dbintegrated")
@EnableJpaRepositories(basePackages = "com.rehearsal.datasource.dbintegrated")
public class JpaConfig {}
