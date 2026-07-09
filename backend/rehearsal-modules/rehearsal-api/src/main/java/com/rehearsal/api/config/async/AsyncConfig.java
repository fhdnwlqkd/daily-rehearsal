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
    // 큐/풀이 모두 찼을 때 호출 스레드(HTTP 요청 스레드)에서 직접 실행하는 CallerRunsPolicy는
    // 이 비동기 전환으로 없애려는 블로킹을 그대로 재현하므로 사용하지 않는다.
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }
}
