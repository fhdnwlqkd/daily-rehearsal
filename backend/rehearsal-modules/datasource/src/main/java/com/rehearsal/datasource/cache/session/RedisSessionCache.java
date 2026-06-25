package com.rehearsal.datasource.cache.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSessionCache implements SessionCache {

  private static final String KEY_PREFIX = "client-session:";
  private static final Duration SESSION_TTL = Duration.ofHours(24);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public ClientSession save(ClientSession session) {
    String key = key(session.getSessionId());
    String json = writeJson(ClientSessionRedisEntity.from(session));
    redisTemplate.opsForValue().set(key, json, SESSION_TTL);
    return session;
  }

  @Override
  public Optional<ClientSession> findById(String sessionId) {
    String json = redisTemplate.opsForValue().get(key(sessionId));
    if (json == null) {
      return Optional.empty();
    }

    return Optional.of(readJson(json).toDomain());
  }

  private String key(String sessionId) {
    return KEY_PREFIX + sessionId;
  }

  private String writeJson(ClientSessionRedisEntity entity) {
    try {
      return objectMapper.writeValueAsString(entity);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize client session.", exception);
    }
  }

  private ClientSessionRedisEntity readJson(String json) {
    try {
      return objectMapper.readValue(json, ClientSessionRedisEntity.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize client session.", exception);
    }
  }
}
