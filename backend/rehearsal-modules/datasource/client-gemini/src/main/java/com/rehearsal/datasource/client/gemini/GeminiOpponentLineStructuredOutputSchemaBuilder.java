package com.rehearsal.datasource.client.gemini;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeminiOpponentLineStructuredOutputSchemaBuilder {

  public Map<String, Object> build() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("sceneCue", stringSchema());
    properties.put("opponentLine", stringSchema());
    properties.put("actionPrompt", stringSchema());
    properties.put("acceptedIntentHint", stringSchema());

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("type", "object");
    root.put("additionalProperties", false);
    root.put("properties", properties);
    root.put("required", List.of("sceneCue", "opponentLine", "actionPrompt", "acceptedIntentHint"));
    return root;
  }

  private Map<String, Object> stringSchema() {
    return Map.of("type", "string");
  }
}
