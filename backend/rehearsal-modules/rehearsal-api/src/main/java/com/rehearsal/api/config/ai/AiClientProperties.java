package com.rehearsal.api.config.ai;

import com.rehearsal.datasource.client.gemini.GeminiGenerateContentConfigBuilder;
import com.rehearsal.datasource.client.gemini.GeminiSlotExtractorClient;
import com.rehearsal.datasource.client.openai.OpenAiResponseCreateParamsBuilder;
import com.rehearsal.datasource.client.openai.OpenAiSlotExtractorClient;
import com.rehearsal.domain.core.annotation.Description;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Description("yml에서 AI provider와 task별 provider/model route를 바인딩하는 설정 모델")
@Getter
@Setter
@ConfigurationProperties(prefix = "rehearsal.ai")
public class AiClientProperties {

  private Defaults defaults = new Defaults();
  private Map<String, TaskRoute> tasks = defaultTasks();
  private OpenAi openai = new OpenAi();
  private Gemini gemini = new Gemini();

  public enum Provider {
    NONE,
    FAKE,
    OPENAI,
    GEMINI
  }

  @Getter
  @Setter
  public static class Defaults {

    private Provider provider = Provider.NONE;
  }

  @Getter
  @Setter
  public static class TaskRoute {

    private Provider provider;
    private String model = "";
  }

  @Getter
  @Setter
  public static class OpenAi {

    private String apiKey = "";
    private String model = OpenAiSlotExtractorClient.DEFAULT_MODEL;
    private long maxOutputTokens = OpenAiResponseCreateParamsBuilder.DEFAULT_MAX_OUTPUT_TOKENS;
    private double temperature = OpenAiResponseCreateParamsBuilder.DEFAULT_TEMPERATURE;
    private boolean store = OpenAiResponseCreateParamsBuilder.DEFAULT_STORE;
  }

  @Getter
  @Setter
  public static class Gemini {

    private String apiKey = "";
    private String model = GeminiSlotExtractorClient.DEFAULT_MODEL;
    private float temperature = GeminiGenerateContentConfigBuilder.DEFAULT_TEMPERATURE;
    private int thinkingBudget = GeminiGenerateContentConfigBuilder.DEFAULT_THINKING_BUDGET;
  }

  private static Map<String, TaskRoute> defaultTasks() {
    Map<String, TaskRoute> tasks = new LinkedHashMap<>();
    tasks.put(AiTask.SLOT_EXTRACTION.key(), new TaskRoute());
    return tasks;
  }
}
