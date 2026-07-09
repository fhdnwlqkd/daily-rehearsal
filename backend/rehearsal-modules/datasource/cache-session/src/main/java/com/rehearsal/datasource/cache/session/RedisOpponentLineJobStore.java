package com.rehearsal.datasource.cache.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.domain.rehearsal.model.OpponentLineJob;
import com.rehearsal.domain.rehearsal.port.OpponentLineJobStore;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisOpponentLineJobStore implements OpponentLineJobStore {

  private static final String KEY_PREFIX = "next-line:";
  private static final Duration JOB_TTL = Duration.ofMinutes(10);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void save(OpponentLineJob job) {
    String key = key(job.sessionId(), job.turnNo());
    redisTemplate.opsForValue().set(key, writeJson(job), JOB_TTL);
  }

  @Override
  public Optional<OpponentLineJob> findById(String sessionId, int turnNo) {
    String json = redisTemplate.opsForValue().get(key(sessionId, turnNo));
    if (json == null) {
      return Optional.empty();
    }

    return Optional.of(readJson(json));
  }

  private String key(String sessionId, int turnNo) {
    return KEY_PREFIX + sessionId + ":" + turnNo;
  }

  private String writeJson(OpponentLineJob job) {
    try {
      return objectMapper.writeValueAsString(job);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize next opponent line job.", exception);
    }
  }

  private OpponentLineJob readJson(String json) {
    try {
      return objectMapper.readValue(json, OpponentLineJob.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize next opponent line job.", exception);
    }
  }
}
