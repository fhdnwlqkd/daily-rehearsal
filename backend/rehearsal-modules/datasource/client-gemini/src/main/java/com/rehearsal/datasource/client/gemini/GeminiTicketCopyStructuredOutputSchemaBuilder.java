package com.rehearsal.datasource.client.gemini;

import com.rehearsal.domain.core.annotation.Description;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Description("Builds the Gemini JSON schema for a ticket change card")
public class GeminiTicketCopyStructuredOutputSchemaBuilder {

  public Map<String, Object> build() {
    Map<String, Object> changeCardProperties = new LinkedHashMap<>();
    changeCardProperties.put("todayAction", stringSchema());
    changeCardProperties.put("tomorrowAttitude", stringSchema());
    changeCardProperties.put("ifThenPlan", stringSchema());

    Map<String, Object> changeCardSchema = new LinkedHashMap<>();
    changeCardSchema.put("type", "object");
    changeCardSchema.put("additionalProperties", false);
    changeCardSchema.put("properties", changeCardProperties);
    changeCardSchema.put("required", List.of("todayAction", "tomorrowAttitude", "ifThenPlan"));

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("changeCard", changeCardSchema);

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("type", "object");
    root.put("additionalProperties", false);
    root.put("properties", properties);
    root.put("required", List.of("changeCard"));
    return root;
  }

  private Map<String, Object> stringSchema() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "string");
    return schema;
  }
}
