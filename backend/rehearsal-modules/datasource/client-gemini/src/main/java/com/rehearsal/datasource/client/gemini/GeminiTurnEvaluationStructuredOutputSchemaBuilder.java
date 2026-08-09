package com.rehearsal.datasource.client.gemini;

import com.rehearsal.domain.core.annotation.Description;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Description("turn evaluation 응답 스키마를 Gemini responseJsonSchema용 JSON Schema map으로 만드는 서비스")
public class GeminiTurnEvaluationStructuredOutputSchemaBuilder {

  public Map<String, Object> build() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("success", booleanSchema());
    properties.put("feedback", stringSchema());

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("type", "object");
    root.put("additionalProperties", false);
    root.put("properties", properties);
    root.put("required", List.of("success", "feedback"));
    return root;
  }

  private Map<String, Object> booleanSchema() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "boolean");
    return schema;
  }

  private Map<String, Object> stringSchema() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "string");
    return schema;
  }
}
