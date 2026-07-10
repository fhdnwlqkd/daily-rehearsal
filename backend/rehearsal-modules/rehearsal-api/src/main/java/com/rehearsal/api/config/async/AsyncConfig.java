package com.rehearsal.api.config.async;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  public static final String TURN_EVALUATION_EXECUTOR = "turnEvaluationExecutor";
  public static final String NEXT_LINE_EXECUTOR = "nextLineExecutor";
  public static final String CONTEXT_EXTRACTION_EXECUTOR = "contextExtractionExecutor";

  @Bean(TURN_EVALUATION_EXECUTOR)
  public ThreadPoolTaskExecutor turnEvaluationExecutor() {
    return executor("turn-eval-");
  }

  @Bean(CONTEXT_EXTRACTION_EXECUTOR)
  public ThreadPoolTaskExecutor contextExtractionExecutor() {
    return executor("context-extract-");
  }

  private ThreadPoolTaskExecutor executor(String threadNamePrefix) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix(threadNamePrefix);
    // Abort instead of running heavy AI work on the HTTP request thread.
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  @Bean(NEXT_LINE_EXECUTOR)
  public ThreadPoolTaskExecutor nextLineExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(20);
    executor.setThreadNamePrefix("next-line-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }
}
