package com.rehearsal.datasource.cache.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationJobStore;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisTurnEvaluationJobStore implements TurnEvaluationJobStore {

  private static final String KEY_PREFIX = "turn-evaluation:";
  private static final Duration JOB_TTL = Duration.ofMinutes(10);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void save(TurnEvaluationJob job) {
    String key = key(job.sessionId(), job.turnNo());
    redisTemplate.opsForValue().set(key, writeJson(job), JOB_TTL);
  }

  @Override
  public Optional<TurnEvaluationJob> findById(String sessionId, int turnNo) {
    String json = redisTemplate.opsForValue().get(key(sessionId, turnNo));
    if (json == null) {
      return Optional.empty();
    }

    return Optional.of(readJson(json));
  }

  private String key(String sessionId, int turnNo) {
    return KEY_PREFIX + sessionId + ":" + turnNo;
  }

  private String writeJson(TurnEvaluationJob job) {
    try {
      return objectMapper.writeValueAsString(job);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize turn evaluation job.", exception);
    }
  }

  private TurnEvaluationJob readJson(String json) {
    try {
      return objectMapper.readValue(json, TurnEvaluationJob.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize turn evaluation job.", exception);
    }
  }
}
