package com.rehearsal.domain.session.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.LinkedHashMap;
import java.util.Map;

@Description("Session context values collected from briefing and follow-up slot extraction")
public class SessionContext {

  public static final String SITUATION_TYPE_KEY = "situation_type";

  private final SituationType situationType;
  private final Map<String, Object> values;

  private SessionContext(SituationType situationType, Map<String, Object> values) {
    this.situationType = situationType;
    this.values = sanitize(values);
  }

  public static SessionContext empty(SituationType situationType) {
    return new SessionContext(situationType, Map.of());
  }

  public static SessionContext from(SituationType situationType, Map<String, Object> values) {
    return new SessionContext(situationType, values);
  }

  public SessionContext merge(Map<String, Object> extractedValues) {
    Map<String, Object> merged = values();
    if (extractedValues != null) {
      for (Map.Entry<String, Object> entry : extractedValues.entrySet()) {
        if (!SITUATION_TYPE_KEY.equals(entry.getKey())) {
          merged.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return new SessionContext(situationType, merged);
  }

  public SituationType situationType() {
    return situationType;
  }

  public Map<String, Object> values() {
    return new LinkedHashMap<>(values);
  }

  public Map<String, Object> valuesWithSituationType() {
    Map<String, Object> context = values();
    context.put(SITUATION_TYPE_KEY, situationType.key());
    return context;
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  private Map<String, Object> sanitize(Map<String, Object> source) {
    Map<String, Object> sanitized = new LinkedHashMap<>();
    if (source == null) {
      return sanitized;
    }
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      if (!SITUATION_TYPE_KEY.equals(entry.getKey())) {
        sanitized.put(entry.getKey(), entry.getValue());
      }
    }
    return sanitized;
  }
}
