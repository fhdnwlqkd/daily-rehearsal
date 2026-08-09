package com.rehearsal.datasource.cache.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.port.TicketJobStore;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisTicketJobStore implements TicketJobStore {

  private static final String KEY_PREFIX = "ticket:";
  private static final Duration JOB_TTL = Duration.ofMinutes(10);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void save(TicketJob job) {
    redisTemplate.opsForValue().set(key(job.sessionId()), writeJson(job), JOB_TTL);
  }

  @Override
  public Optional<TicketJob> findById(String sessionId) {
    String json = redisTemplate.opsForValue().get(key(sessionId));
    if (json == null) {
      return Optional.empty();
    }

    return Optional.of(readJson(json));
  }

  private String key(String sessionId) {
    return KEY_PREFIX + sessionId;
  }

  private String writeJson(TicketJob job) {
    try {
      return objectMapper.writeValueAsString(job);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize ticket job.", exception);
    }
  }

  private TicketJob readJson(String json) {
    try {
      return objectMapper.readValue(json, TicketJob.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize ticket job.", exception);
    }
  }
}
