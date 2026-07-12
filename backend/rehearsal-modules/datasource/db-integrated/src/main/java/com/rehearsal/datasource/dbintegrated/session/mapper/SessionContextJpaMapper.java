package com.rehearsal.datasource.dbintegrated.session.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.datasource.dbintegrated.session.entity.SessionContextValueJpaEntity;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionContextJpaMapper {

  private final ObjectMapper objectMapper;

  public String writeValue(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize session context value.", exception);
    }
  }

  public SessionContext toDomain(
      SituationType situationType, List<SessionContextValueJpaEntity> entities) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (SessionContextValueJpaEntity entity : entities) {
      values.put(entity.getContextKey(), readValue(entity.getContextValue()));
    }
    return SessionContext.from(situationType, values);
  }

  private Object readValue(String value) {
    try {
      return objectMapper.readValue(value, Object.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize session context value.", exception);
    }
  }
}
