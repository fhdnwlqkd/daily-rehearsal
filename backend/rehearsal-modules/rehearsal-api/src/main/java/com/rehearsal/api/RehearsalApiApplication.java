package com.rehearsal.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.rehearsal")
public class RehearsalApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(RehearsalApiApplication.class, args);
  }
}
