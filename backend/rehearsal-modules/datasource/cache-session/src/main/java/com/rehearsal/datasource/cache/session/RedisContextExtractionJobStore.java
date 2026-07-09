package com.rehearsal.datasource.cache.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.port.ContextExtractionJobStore;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisContextExtractionJobStore implements ContextExtractionJobStore {

  private static final String KEY_PREFIX = "context-extraction:";
  private static final Duration JOB_TTL = Duration.ofMinutes(10);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void save(ContextExtractionJob job) {
    redisTemplate.opsForValue().set(key(job.sessionId(), job.jobId()), writeJson(job), JOB_TTL);
  }

  @Override
  public Optional<ContextExtractionJob> findById(String sessionId, String jobId) {
    String json = redisTemplate.opsForValue().get(key(sessionId, jobId));
    if (json == null) {
      return Optional.empty();
    }
    return Optional.of(readJson(json).toDomain());
  }

  private String key(String sessionId, String jobId) {
    return KEY_PREFIX + sessionId + ":" + jobId;
  }

  private String writeJson(ContextExtractionJob job) {
    try {
      return objectMapper.writeValueAsString(ContextExtractionJobRedisEntity.from(job));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize context extraction job.", exception);
    }
  }

  private ContextExtractionJobRedisEntity readJson(String json) {
    try {
      return objectMapper.readValue(json, ContextExtractionJobRedisEntity.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize context extraction job.", exception);
    }
  }
}
